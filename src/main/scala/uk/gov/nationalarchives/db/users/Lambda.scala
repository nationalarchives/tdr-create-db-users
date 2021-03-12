package uk.gov.nationalarchives.db.users

import scalikejdbc._
import Config._
import software.amazon.awssdk.core.interceptor.Context

import java.io.{InputStream, OutputStream}
import java.nio.charset.Charset

class Lambda {

  def process(inputStream: InputStream, outputStream: OutputStream, context: Context) = {
    println("Running")
    val apiUser = createUser("api_user", lambdaConfig.db.passwords.api)
    sql"GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO $apiUser;".execute.apply()
    val migrationsUser = createUser("migrations_user", lambdaConfig.db.passwords.migrations)
    sql"GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $migrationsUser;".execute.apply()
    outputStream.write("Users created successfully".getBytes(Charset.defaultCharset()))
  }

  def createUser(username: String, password: String): SQLSyntax = {
    //createUnsafely is needed as the usual interpolation returns ERROR: syntax error at or near "$1"
    //There is a similar issue here https://github.com/scalikejdbc/scalikejdbc/issues/320
    val user = sqls.createUnsafely(username)
    val sqlPassword = sqls.createUnsafely(password)
    sql"CREATE USER $user WITH ENCRYPTED PASSWORD '$sqlPassword'".execute().apply()
    sql"GRANT CONNECT ON DATABASE consignmentapi TO $user;".execute.apply()
    sql"GRANT USAGE ON SCHEMA public TO $user;".execute.apply()
    user
  }
}
