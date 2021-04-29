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
    //Create the extension. This needs to be done by the admin user and this is the only script run by the admin user.
    val uuid = sqls.createUnsafely("uuid-ossp")
    sql"""CREATE EXTENSION IF NOT EXISTS "$uuid" ;""".execute().apply()

    val migrationsUser = createConsignmentApiUser(lambdaConfig.migrationsUser)
    sql"GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $migrationsUser;".execute.apply()
    sql"GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO $migrationsUser;".execute.apply()

    val apiUser = createConsignmentApiUser(lambdaConfig.consignmentApiUser)

    //Switch to migrations user to set permissions on it's own tables correctly
    sql"SET ROLE $migrationsUser".execute().apply()
    sql"GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO $apiUser;".execute.apply()
    sql"GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO $apiUser;".execute.apply()

    //Grants permissions for any new tables that are created.
    //This is not needed for the migrations user as it will be creating the tables so it will own them/
    sql"ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE ON TABLES TO $apiUser;".execute().apply()
  }

  def grantConnectAndUsage(user: SQLSyntax, database: SQLSyntax) = {
    sql"GRANT CONNECT ON DATABASE $database TO $user;".execute.apply()
    sql"GRANT USAGE ON SCHEMA public TO $user;".execute.apply()
  }

  def createConsignmentApiUser(username: String): SQLSyntax = {
    //createUnsafely is needed as the usual interpolation returns ERROR: syntax error at or near "$1"
    //    There is a similar issue here https://github.com/scalikejdbc/scalikejdbc/issues/320
    val user = sqls.createUnsafely(username)
    val password = sqls.createUnsafely("password")
    sql"CREATE USER $user WITH PASSWORD '$password'".execute().apply()
    grantConnectAndUsage(user, sqls.createUnsafely("consignmentapi"))
    sql"GRANT rds_iam TO $user;".execute().apply()
    user
  }
}
