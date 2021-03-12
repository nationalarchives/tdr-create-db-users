import sbt._

object Dependencies {
  lazy val postgres = "org.postgresql" % "postgresql" % "42.2.19"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.2"
  lazy val scalikeJdbc = "org.scalikejdbc" %% "scalikejdbc" % "3.5.0"
  lazy val pureConfig = "com.github.pureconfig" %% "pureconfig" % "0.14.1"
}
