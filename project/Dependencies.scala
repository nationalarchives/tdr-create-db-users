import sbt._

object Dependencies {
  lazy val h2 = "com.h2database" % "h2" % "1.4.200"
  lazy val postgres = "org.postgresql" % "postgresql" % "42.2.19"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.2"
  lazy val scalikeJdbc = "org.scalikejdbc" %% "scalikejdbc" % "3.5.0"
  lazy val ssm = "software.amazon.awssdk" % "ssm" % "2.16.16"
  lazy val pureConfig = "com.github.pureconfig" %% "pureconfig" % "0.14.1"
  lazy val wiremock = "com.github.tomakehurst" % "wiremock" % "2.27.2"
}
