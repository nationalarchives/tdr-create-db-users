package uk.gov.nationalarchives.db.users

import pureconfig._
import pureconfig.generic.auto._
import scalikejdbc.{AutoSession, ConnectionPool}
import uk.gov.nationalarchives.aws.utils.Clients.kms
import uk.gov.nationalarchives.aws.utils.KMSUtils

object Config {
  case class LambdaConfig(driver: String, username: String, password: String, url: String, consignmentApiUser: String, migrationsUser: String, functionName: String, kmsEndpoint: String)

  val lambdaConfig: LambdaConfig = ConfigSource.default.load[LambdaConfig] match {
    case Left(error) => throw new RuntimeException(error.prettyPrint(0))
    case Right(value) => value
  }

  val kmsUtils: KMSUtils = KMSUtils(kms(lambdaConfig.kmsEndpoint), Map("LambdaFunctionName" -> lambdaConfig.functionName))

  implicit val session: AutoSession.type = AutoSession
  Class.forName(lambdaConfig.driver)
  ConnectionPool.singleton(kmsUtils.decryptValue(lambdaConfig.url), kmsUtils.decryptValue(lambdaConfig.username), kmsUtils.decryptValue(lambdaConfig.password))
}
