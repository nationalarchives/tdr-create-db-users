import sbt._

object Dependencies {
  lazy val awsUtils =  "uk.gov.nationalarchives" %% "tdr-aws-utils" % "0.1.29"
  lazy val postgres = "org.postgresql" % "postgresql" % "42.3.5"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.12"
  lazy val scalikeJdbc = "org.scalikejdbc" %% "scalikejdbc" % "4.0.0"
  lazy val pureConfig = "com.github.pureconfig" %% "pureconfig" % "0.17.1"
  lazy val wiremock = "com.github.tomakehurst" % "wiremock" % "2.27.2"
}
