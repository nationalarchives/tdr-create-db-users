import sbt._

object Dependencies {
  lazy val awsUtils =  "uk.gov.nationalarchives.aws.utils" %% "tdr-aws-utils" % "0.1.15"
  lazy val postgres = "org.postgresql" % "postgresql" % "42.3.4"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.11"
  lazy val scalikeJdbc = "org.scalikejdbc" %% "scalikejdbc" % "4.0.0"
  lazy val pureConfig = "com.github.pureconfig" %% "pureconfig" % "0.17.1"
  lazy val wiremock = "com.github.tomakehurst" % "wiremock" % "2.27.2"
}
