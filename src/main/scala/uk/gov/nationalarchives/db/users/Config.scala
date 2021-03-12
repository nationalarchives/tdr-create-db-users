package uk.gov.nationalarchives.db.users

import pureconfig._
import pureconfig.generic.auto._
import scalikejdbc.{AutoSession, ConnectionPool}

object Config {
  case class Passwords(api: String, migrations: String)
  case class DbConfig(driver: String, username: String, password: String, url: String, passwords: Passwords)
  case class LambdaConfig(db: DbConfig)

  val lambdaConfig: LambdaConfig = ConfigSource.default.load[LambdaConfig] match {
    case Left(error) => throw new RuntimeException(error.prettyPrint(0))
    case Right(value) =>value
  }

  implicit val session: AutoSession.type = AutoSession
  Class.forName(lambdaConfig.db.driver)
  ConnectionPool.singleton(lambdaConfig.db.url, lambdaConfig.db.username, lambdaConfig.db.password)
}
