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
  crossScalaVersions := Seq("2.10.6", "2.11.8"),
  resolvers ++=  Seq(
    socrata_maven,
    socrata_ivy
  ),
  scalastyleFailOnError in Compile := false
)

lazy val eurybates = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "eurybates",
    crossScalaVersions := Seq("2.10.6", "2.11.7"),
    libraryDependencies ++= Seq(
      activemq,
      kafka_clients,
      rojoma_json,
      socrata_zookeeper,
      scala_test,
      scala_logging
    )
  )


// TODO: enable static analysis build failures
// TODO: Unable to incorporate in common settings....????
com.socrata.sbtplugins.findbugs.JavaFindBugsPlugin.JavaFindBugsKeys.findbugsFailOnError in Compile := false
com.socrata.sbtplugins.findbugs.JavaFindBugsPlugin.JavaFindBugsKeys.findbugsFailOnError in Test := false
