package uk.gov.nationalarchives.db.users

import pureconfig._
import pureconfig.generic.auto._
import scalikejdbc.{AutoSession, ConnectionPool}

object Config {
  case class LambdaConfig(driver: String, username: String, password: String, url: String, consignmentApiUser: String, migrationsUser: String)

  val lambdaConfig: LambdaConfig = ConfigSource.default.load[LambdaConfig] match {
    case Left(error) => throw new RuntimeException(error.prettyPrint(0))
    case Right(value) => value
  }

  implicit val session: AutoSession.type = AutoSession
  Class.forName(lambdaConfig.driver)
  ConnectionPool.singleton(lambdaConfig.url, lambdaConfig.username, lambdaConfig.password)
}
