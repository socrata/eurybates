organization := "com.socrata"

name := "eurybates"

scalaVersion := "2.12.8"

crossScalaVersions := Seq("2.10.6", "2.11.8", scalaVersion.value)

resolvers += "socrata" at "https://repo.socrata.com/artifactory/libs-release"

mimaPreviousArtifacts := Set("com.socrata" %% "eurybates" % "2.1.0")

libraryDependencies ++= Seq(
  "com.rojoma" %% "rojoma-json-v3" % "3.10.0",
  "com.socrata" %% "socrata-zookeeper" % "1.1.0",
  "org.apache.activemq" % "activemq-core" % "5.7.0" % "optional",
  "org.apache.kafka" % "kafka-clients" % "0.8.2.1" % "optional",
  "org.slf4j" % "slf4j-api" % "1.7.21",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test"
)

publishTo := {
  val nexus = "https://repo.socrata.com/artifactory/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "libs-snapshot-local")
  else
    Some("releases" at nexus + "libs-release-local")
}
