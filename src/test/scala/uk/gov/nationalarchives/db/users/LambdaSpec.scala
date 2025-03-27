package uk.gov.nationalarchives.db.users

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import Config._
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.{post, urlEqualTo}
import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.{Parameters, ResponseDefinitionTransformer}
import com.github.tomakehurst.wiremock.http.{Request, ResponseDefinition}
import scalikejdbc._

import java.nio.ByteBuffer
import java.nio.charset.Charset
import io.circe.generic.auto._
import io.circe.parser.decode
import org.scalatest.Assertion

import scala.util.Try

class LambdaSpec extends AnyFlatSpec with Matchers {

  val kmsWiremock = new WireMockServer(new WireMockConfiguration().port(9001).extensions(new ResponseDefinitionTransformer {
    override def transform(request: Request, responseDefinition: ResponseDefinition, files: FileSource, parameters: Parameters): ResponseDefinition = {
      case class KMSRequest(CiphertextBlob: String)
      decode[KMSRequest](request.getBodyAsString) match {
        case Left(err) => throw err
        case Right(req) =>
          val charset = Charset.defaultCharset()
          val plainText = charset.newDecoder.decode(ByteBuffer.wrap(req.CiphertextBlob.getBytes(charset))).toString
          ResponseDefinitionBuilder
            .like(responseDefinition)
            .withBody(s"""{"Plaintext": "$plainText"}""")
            .build()
      }
    }
    override def getName: String = ""
  }))


  val secretsManagerWiremock = new WireMockServer(new WireMockConfiguration().port(9002).extensions(new ResponseDefinitionTransformer {
    override def transform(request: Request, responseDefinition: ResponseDefinition, files: FileSource, parameters: Parameters): ResponseDefinition = {
      case class SecretsManagerRequest(CiphertextBlob: String)
      decode[SecretsManagerRequest](request.getBodyAsString) match {
        case Left(err) => throw err
        case Right(_) =>
          ResponseDefinitionBuilder
            .like(responseDefinition)
            .withBody(s"""{"username": "username", "password": "password"}""")
            .build()
      }
    }
    override def getName: String = ""
  }))


  def prepareKmsMock(): Unit = {
    kmsWiremock.stubFor(post(urlEqualTo("/")))
    kmsWiremock.start()
  }

  def prepareSecretsManagerMock(): Unit = {
    secretsManagerWiremock.stubFor(post(urlEqualTo("/")))
    secretsManagerWiremock.start()
  }

  def prepareKeycloakDb(username: String): AnyVal = {
    val user = sqls.createUnsafely(username)
    sql"SET ROLE tdr".execute()
    Try(sql"CREATE DATABASE keycloak;".execute())
    sql"CREATE TABLE IF NOT EXISTS Test();".execute()
    val userCount = sql"SELECT count(*) as userCount FROM pg_roles WHERE rolname = $username".map(_.int("userCount")).list.apply.head
    if(userCount > 0) {
      sql"REVOKE CONNECT ON DATABASE keycloak FROM $user;".execute.apply()
      sql"REVOKE USAGE ON SCHEMA public FROM $user;".execute.apply()
      sql"REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM $user;".execute.apply()
      sql"DROP USER IF EXISTS $user;".execute()
    }
  }

    def prepareConsignmentDb(username: String): AnyVal = {
    val user = sqls.createUnsafely(username)
    sql"DROP ROLE IF EXISTS rds_iam".execute()
    sql"CREATE ROLE rds_iam".execute()
    sql"CREATE SEQUENCE IF NOT EXISTS consignment_sequence_id;".execute()
    val userCount = sql"SELECT count(*) as userCount FROM pg_roles WHERE rolname = $username".map(_.int("userCount")).list.apply.head
    if (userCount > 0) {
      sql"SET ROLE tdr;".execute()
      sql"REVOKE CONNECT ON DATABASE consignmentapi FROM $user;".execute.apply()
      sql"REVOKE USAGE ON SCHEMA public FROM $user;".execute.apply()
      sql"SET ROLE migrations_user".execute()
      sql"ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE SELECT, INSERT, UPDATE ON TABLES FROM $user;".execute()
      sql"ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL PRIVILEGES ON SEQUENCES FROM $user;".execute()
      sql"SET ROLE tdr".execute()
      sql"REVOKE ALL PRIVILEGES ON consignment_sequence_id FROM $user;".execute()
      sql"REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM $user;".execute.apply()
      sql"DROP TABLE IF EXISTS test;".execute()
      sql"DROP SEQUENCE IF EXISTS consignment_sequence_id".execute()
      sql"DROP USER IF EXISTS $user;".execute()
    }
  }

  def checkPrivileges(username: String, expectedPrivileges: List[String]): Assertion = {
    sql"SET ROLE migrations_user;".execute()
    val privileges: List[String] = sql"SELECT privilege_type FROM information_schema.table_privileges WHERE grantee=$username"
      .map(rs => rs.string("privilege_type"))
      .list.apply()
    privileges.sorted should equal(expectedPrivileges)
  }

  def createTable(username: String) = {
    val user = sqls.createUnsafely(username)
    sql"SET ROLE $user;".execute()
    sql"CREATE TABLE IF NOT EXISTS Test();".execute()
  }

  "The process method" should "create the users with the correct parameters in the consignment database" in {
    prepareKmsMock()
    prepareSecretsManagerMock()
    prepareConsignmentDb(lambdaConfig.appConfig.consignmentApiUser)
    prepareConsignmentDb(lambdaConfig.appConfig.migrationsUser)
    new Lambda().createUsers("consignmentapi")
    createTable(lambdaConfig.appConfig.migrationsUser)
    checkPrivileges(lambdaConfig.appConfig.consignmentApiUser, List("DELETE", "INSERT", "SELECT", "UPDATE"))
    checkPrivileges(lambdaConfig.appConfig.migrationsUser, List("DELETE", "INSERT", "REFERENCES", "SELECT", "TRIGGER", "TRUNCATE", "UPDATE"))
    kmsWiremock.stop()
  }

  "The process method" should "create the users with the correct parameters in the keycloak database" in {
    prepareKmsMock()
    prepareSecretsManagerMock()
    prepareKeycloakDb(lambdaConfig.appConfig.keycloakUser)
    new Lambda().createUsers("keycloak")
    createTable(lambdaConfig.appConfig.keycloakUser)
    checkPrivileges(lambdaConfig.appConfig.keycloakUser, List("DELETE", "INSERT", "REFERENCES", "SELECT", "TRIGGER", "TRUNCATE", "UPDATE"))
  }

  "The process method" should "create the users with the correct parameters for the bastion user" in {
    sql"SET ROLE tdr;".execute()
    prepareKmsMock()
    prepareSecretsManagerMock()
    prepareConsignmentDb(lambdaConfig.appConfig.bastionUser)
    createTable(lambdaConfig.appConfig.migrationsUser)
    sql"SET ROLE tdr;".execute()
    new Lambda().createUsers("bastion")
    checkPrivileges(lambdaConfig.appConfig.bastionUser, List("SELECT"))
    kmsWiremock.stop()
  }
}
