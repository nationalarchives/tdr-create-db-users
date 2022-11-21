package uk.gov.nationalarchives.db.users

import pureconfig._
import pureconfig.generic.auto._
import scalikejdbc.{AutoSession, ConnectionPool}
import uk.gov.nationalarchives.aws.utils.kms.KMSClients.kms
import uk.gov.nationalarchives.aws.utils.kms.KMSUtils

object Config {

  case class LambdaConfig(driver: String, username: String, password: String, url: String, consignmentApiUser: String, migrationsUser: String, functionName: String, kmsEndpoint: String, databaseName: String, keycloakUser: String, keycloakPassword: Option[String], bastionUser: String)

  val lambdaConfig: LambdaConfig = ConfigSource.default.load[LambdaConfig] match {
    case Left(error) => throw new RuntimeException(error.prettyPrint(0))
    case Right(value) =>
      val kmsUtils: KMSUtils = KMSUtils(kms(value.kmsEndpoint), Map("LambdaFunctionName" -> value.functionName))
      value.copy(
        username = kmsUtils.decryptValue(value.username),
        password = kmsUtils.decryptValue(value.password),
        url = kmsUtils.decryptValue(value.url),
        databaseName = kmsUtils.decryptValue(value.databaseName),
        keycloakPassword = value.keycloakPassword.map(password => kmsUtils.decryptValue(password))
      )
  }

  implicit val session: AutoSession.type = AutoSession
  Class.forName(lambdaConfig.driver)
  ConnectionPool.singleton(lambdaConfig.url, lambdaConfig.username, lambdaConfig.password)
}
