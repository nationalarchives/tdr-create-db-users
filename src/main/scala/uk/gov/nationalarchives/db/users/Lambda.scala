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
      case "bastion" => createBastionUser
    }
  }

  def createBastionUser: Boolean = {
    val user = createIamAuthenticationUser(lambdaConfig.bastionUser, "consignmentapi")
    //Grant access to tables created before we started using the migrations user
    sql"GRANT SELECT ON ALL TABLES IN SCHEMA public TO $user;".execute()
    sql"GRANT SELECT ON ALL TABLES IN SCHEMA public TO $user;".execute()
    sql"SET ROLE migrations_user;".execute()
    //Grant access to tables created by the migrations user.
    sql"GRANT SELECT ON ALL TABLES IN SCHEMA public TO $user;".execute()
  }

  def createKeycloakUser: Boolean = {
    val user = createIamAuthenticationUser(lambdaConfig.keycloakUser, "keycloak")

    grantConnectAndUsage(user, sqls.createUnsafely("keycloak"))
    sql"GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $user;".execute.apply()
    //Grant access to new tables. Keycloak upgrades sometimes create new tables
    sql"ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES to $user".execute()
  }

  def createConsignmentApiUsers: Boolean = {
    //Create the extension. This needs to be done by the admin user and this is the only script run by the admin user.
    val databaseName = "consignmentapi"
    val uuid = sqls.createUnsafely("uuid-ossp")
    sql"""CREATE EXTENSION IF NOT EXISTS "$uuid" ;""".execute()

    val migrationsUser = createIamAuthenticationUser(lambdaConfig.migrationsUser, databaseName)
    sql"GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $migrationsUser;".execute.apply()
    sql"GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO $migrationsUser;".execute.apply()

    val apiUser = createIamAuthenticationUser(lambdaConfig.consignmentApiUser, databaseName)

    //Switch to migrations user to set permissions on it's own tables correctly
    sql"SET ROLE $migrationsUser".execute()
    sql"GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO $apiUser;".execute.apply()
    sql"GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO $apiUser;".execute.apply()

    //Grants permissions for any new tables that are created.
    //This is not needed for the migrations user as it will be creating the tables so it will own them/
    sql"ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE ON TABLES TO $apiUser;".execute()
    sql"ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO $apiUser;".execute()
  }

  def grantConnectAndUsage(user: SQLSyntax, database: SQLSyntax) = {
    sql"GRANT CONNECT ON DATABASE $database TO $user;".execute.apply()
    sql"GRANT USAGE ON SCHEMA public TO $user;".execute.apply()
  }

  def createIamAuthenticationUser(username: String, databaseName: String): SQLSyntax = {
    //createUnsafely is needed as the usual interpolation returns ERROR: syntax error at or near "$1"
    //There is a similar issue here https://github.com/scalikejdbc/scalikejdbc/issues/320
    val user = sqls.createUnsafely(username)
    sql"CREATE USER $user".execute()
    grantConnectAndUsage(user, sqls.createUnsafely(databaseName))
    sql"GRANT rds_iam TO $user;".execute()
    user
  }
}
