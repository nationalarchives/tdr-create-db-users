package uk.gov.nationalarchives.db.users

import scalikejdbc._
import uk.gov.nationalarchives.db.users.Config._

import java.io.{InputStream, OutputStream}
import java.nio.charset.Charset

class Lambda {

  def process(inputStream: InputStream, outputStream: OutputStream): Unit = {
    createUsers(lambdaConfig.databaseName)

    outputStream.write("Users created successfully".getBytes(Charset.defaultCharset()))
  }

  def createUsers(databaseName: String): Boolean = {
    databaseName match {
      case "consignmentapi" => createConsignmentApiUsers
      case "keycloak" => createKeycloakUser
    }
  }

  def createKeycloakUser: Boolean = {
    val user = sqls.createUnsafely(lambdaConfig.keycloakUser)
    val keycloakPassword = lambdaConfig.keycloakPassword match {
      case Some(value) => value
      case None => throw new RuntimeException("Keycloak password has not been provided")
    }
    val password = sqls.createUnsafely(keycloakPassword)
    sql"CREATE USER $user WITH PASSWORD '$password'".execute().apply()
    grantConnectAndUsage(user, sqls.createUnsafely("keycloak"))
    sql"GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $user;".execute.apply()
  }

  def createConsignmentApiUsers: Boolean = {
    val apiUser = createConsignmentApiUser(lambdaConfig.consignmentApiUser)
    sql"GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO $apiUser;".execute.apply()
    sql"GRANT USAGE on consignment_sequence_id to $apiUser;".execute.apply()

    //Grants permissions for any new tables that are created.
    //This is not needed for the migrations user as it will be creating the tables so it will own them/
    sql"ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE ON TABLES TO $apiUser;".execute().apply()

    val migrationsUser = createConsignmentApiUser(lambdaConfig.migrationsUser)
    sql"GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $migrationsUser;".execute.apply()
    sql"GRANT ALL PRIVILEGES ON consignment_sequence_id TO $migrationsUser;".execute.apply()
  }

  def grantConnectAndUsage(user: SQLSyntax, database: SQLSyntax) = {
    sql"GRANT CONNECT ON DATABASE $database TO $user;".execute.apply()
    sql"GRANT USAGE ON SCHEMA public TO $user;".execute.apply()
  }

  def createConsignmentApiUser(username: String): SQLSyntax = {
    //createUnsafely is needed as the usual interpolation returns ERROR: syntax error at or near "$1"
    //There is a similar issue here https://github.com/scalikejdbc/scalikejdbc/issues/320
    val user = sqls.createUnsafely(username)
    sql"CREATE USER $user".execute().apply()
    grantConnectAndUsage(user, sqls.createUnsafely("consignmentapi"))
    sql"GRANT rds_iam TO $user;".execute().apply()
    user
  }
}
