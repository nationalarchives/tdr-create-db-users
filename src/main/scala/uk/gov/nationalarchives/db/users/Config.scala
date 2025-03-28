package uk.gov.nationalarchives.db.users

import io.circe.{Decoder, parser}
import io.circe.generic.semiauto.deriveDecoder
import pureconfig._
import pureconfig.generic.auto._
import scalikejdbc.{AutoSession, ConnectionPool}
import uk.gov.nationalarchives.aws.utils.kms.KMSClients.kms
import uk.gov.nationalarchives.aws.utils.kms.KMSUtils
import uk.gov.nationalarchives.aws.utils.secretsmanager.SecretsManagerClients.secretsmanager
import uk.gov.nationalarchives.aws.utils.secretsmanager.SecretsManagerUtils

object Config {
  
  case class LambdaConfig(credentials: Credentials, appConfig: ApplicationConfig)
  case class ApplicationConfig(driver: String, dbSecretsArn: String, url: String, consignmentApiUser: String, migrationsUser: String, functionName: String, kmsEndpoint: String, secretsManagerEndpoint: String, databaseName: String, keycloakUser: String, keycloakPassword: Option[String], bastionUser: String)
  case class Credentials(username: String, password: String)

  implicit val credentialsDecoder: Decoder[Credentials] = deriveDecoder[Credentials]

  val lambdaConfig: LambdaConfig = ConfigSource.default.load[ApplicationConfig] match {
    case Left(error) => throw new RuntimeException(error.prettyPrint(0))
    case Right(value) =>
      val kmsUtils: KMSUtils = KMSUtils(kms(value.kmsEndpoint), Map("LambdaFunctionName" -> value.functionName))
      val secretsManagerUtils: SecretsManagerUtils = SecretsManagerUtils(secretsmanager(value.secretsManagerEndpoint))
      val credentialsString = secretsManagerUtils.getSecretValueString(kmsUtils.decryptValue(value.dbSecretsArn))
      val credentials = parser.decode[Credentials](credentialsString)
      credentials match {
        case Left(error) => throw new RuntimeException(error.toString)
        case Right(creds) => 
          LambdaConfig(
          credentials = creds,
          appConfig = value.copy(
            url = kmsUtils.decryptValue(value.url),
            databaseName = kmsUtils.decryptValue(value.databaseName),
            keycloakPassword = value.keycloakPassword.map(password => kmsUtils.decryptValue(password))
          )
        )
      }
  }

  implicit val session: AutoSession.type = AutoSession
  Class.forName(lambdaConfig.appConfig.driver)
  ConnectionPool.singleton(lambdaConfig.appConfig.url, lambdaConfig.credentials.username, lambdaConfig.credentials.password)
}
