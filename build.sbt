import Dependencies._

ThisBuild / scalaVersion     := "2.13.16"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "uk.gov.nationalarchives"
ThisBuild / organizationName := "db.users"

lazy val root = (project in file("."))
  .settings(
    name := "tdr-create-db-users",
    resolvers ++= Seq[Resolver](
      "TDR Releases" at "s3://tdr-releases-mgmt"
    ),
    libraryDependencies ++= Seq(
      kmsUtils,
      secretsManagerUtils,
      pureConfig,
      postgres,
      scalikeJdbc,
      circeCore,
      circeGeneric,
      circeParser,
      scalaTest % Test,
      wiremock % Test
    )
  )

(assembly / assemblyMergeStrategy) := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

(assembly / assemblyJarName) := "create-db-users.jar"

Test / fork := true
Test / envVars := Map("AWS_ACCESS_KEY_ID" -> "test", "AWS_SECRET_ACCESS_KEY" -> "test")

