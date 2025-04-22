import sbt._

object Dependencies {
  private val circeVersion = "0.14.13"

  lazy val circeCore = "io.circe" %% "circe-core" % circeVersion
  lazy val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
  lazy val circeParser = "io.circe" %% "circe-parser" % circeVersion
  lazy val kmsUtils =  "uk.gov.nationalarchives" %% "kms-utils" % "0.1.252"
  lazy val secretsManagerUtils =  "uk.gov.nationalarchives" %% "secretsmanager-utils" % "0.1.252"
  lazy val postgres = "org.postgresql" % "postgresql" % "42.7.5"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.19"
  lazy val scalikeJdbc = "org.scalikejdbc" %% "scalikejdbc" % "4.3.2"
  lazy val pureConfig = "com.github.pureconfig" %% "pureconfig" % "0.17.8"
  lazy val wiremock = "com.github.tomakehurst" % "wiremock" % "3.0.1"
}
