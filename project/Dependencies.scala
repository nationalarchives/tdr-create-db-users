import sbt._

object Dependencies {
  private val circeVersion = "0.14.5"

  lazy val circeCore = "io.circe" %% "circe-core" % circeVersion
  lazy val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
  lazy val circeParser = "io.circe" %% "circe-parser" % circeVersion
  lazy val kmsUtils =  "uk.gov.nationalarchives" %% "kms-utils" % "0.1.83"
  lazy val postgres = "org.postgresql" % "postgresql" % "42.6.0"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.15"
  lazy val scalikeJdbc = "org.scalikejdbc" %% "scalikejdbc" % "4.0.0"
  lazy val pureConfig = "com.github.pureconfig" %% "pureconfig" % "0.17.3"
  lazy val wiremock = "com.github.tomakehurst" % "wiremock" % "2.27.2"
}
