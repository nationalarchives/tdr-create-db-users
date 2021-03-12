package uk.gov.nationalarchives.db.users

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import Config._
import scalikejdbc._

import java.io.ByteArrayOutputStream

class LambdaSpec extends AnyFlatSpec with Matchers {

  def prepareDb(username: String) = {
    val user = sqls.createUnsafely(username)
    sql"CREATE TABLE IF NOT EXISTS Test();".execute().apply()
    val userCount = sql"SELECT count(*) as userCount FROM pg_roles WHERE rolname = $username".map(_.int("userCount")).list.apply.head
    if (userCount > 0) {
      sql"REVOKE CONNECT ON DATABASE consignmentapi FROM $user;".execute.apply()
      sql"REVOKE USAGE ON SCHEMA public FROM $user;".execute.apply()
      sql"REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM $user;".execute.apply()
      sql"DROP USER IF EXISTS $user;".execute().apply()
    }
  }

  def checkPrivileges(username: String, expectedPrivileges: List[String]) = {
    val privileges: List[String] = sql"SELECT privilege_type FROM information_schema.table_privileges WHERE grantee=$username"
      .map(rs => rs.string("privilege_type"))
      .list.apply()
    privileges.sorted should equal(expectedPrivileges)
  }

  "The process method" should s"create the users with the correct parameters" in {
    prepareDb("api_user")
    prepareDb("migrations_user")
    new Lambda().process(null, new ByteArrayOutputStream())
    checkPrivileges("api_user", List("INSERT", "SELECT", "UPDATE"))
    checkPrivileges("migrations_user", List("DELETE", "INSERT", "REFERENCES", "SELECT", "TRIGGER", "TRUNCATE", "UPDATE"))
  }
}
