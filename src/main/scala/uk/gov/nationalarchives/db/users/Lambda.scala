
package uk.gov.nationalarchives.db.users

import scalikejdbc._
import uk.gov.nationalarchives.db.users.Config._

import java.io.{InputStream, OutputStream}
import java.nio.charset.Charset

class Lambda {

  def process(inputStream: InputStream, outputStream: OutputStream) = {
    println("Running")
    val apiUser = createUser("consignment_api_user")
    sql"GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO $apiUser;".execute.apply()
    val migrationsUser = createUser("migrations_user")
    sql"GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $migrationsUser;".execute.apply()
    outputStream.write("Users created successfully".getBytes(Charset.defaultCharset()))
  }

  def createUser(username: String): SQLSyntax = {
    //createUnsafely is needed as the usual interpolation returns ERROR: syntax error at or near "$1"
    //There is a similar issue here https://github.com/scalikejdbc/scalikejdbc/issues/320
    val user = sqls.createUnsafely(username)
    sql"CREATE USER $user".execute().apply()
    sql"GRANT CONNECT ON DATABASE consignmentapi TO $user;".execute.apply()
    sql"GRANT USAGE ON SCHEMA public TO $user;".execute.apply()
    sql"GRANT rds_iam TO $user;".execute().apply()
    user
  }
}
