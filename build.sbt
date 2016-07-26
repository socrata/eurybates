import Dependencies.Resolvers._
import Dependencies._
import sbt.Keys._

/**
  * Setting common to all projects.
  *
  * NOTE: This must be added to all Subprojects!
  */
lazy val commonSettings = Seq(
  organization := "com.socrata",
  scalaVersion := "2.11.8",
  resolvers ++=  Seq(
    socrata_maven,
    socrata_ivy
  ),
  scalastyleFailOnError in Compile := true,
  assemblyMergeStrategy in assembly := {
    case "META-INF/spring.tooling" | "overview.html" => MergeStrategy.last
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)

lazy val eurybates = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "eurybates",
    libraryDependencies ++= Seq(
      activemq,
      kafka_clients,
      rojoma_json,
      socrata_zookeeper,
      scala_test,
      scala_logging
    ),
    publishTo := {
      val nexus = "https://repo.socrata.com/artifactory/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "libs-snapshot-local")
      else
        Some("releases" at nexus + "libs-release-local")
    }
  )


// TODO: enable static analysis build failures
// TODO: Unable to incorporate in common settings....????
com.socrata.sbtplugins.findbugs.JavaFindBugsPlugin.JavaFindBugsKeys.findbugsFailOnError in Compile := false
com.socrata.sbtplugins.findbugs.JavaFindBugsPlugin.JavaFindBugsKeys.findbugsFailOnError in Test := false

