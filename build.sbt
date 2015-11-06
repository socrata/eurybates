name := "eurybates"

scalaVersion := "2.10.6"

libraryDependencies ++= Seq(
  "org.apache.activemq" % "activemq-core" % "5.7.0" % "optional",
  "org.apache.kafka" % "kafka-clients" % "0.8.2.1" % "optional",
  "com.rojoma" %% "rojoma-json-v3" % "[3.2.1,4.0.0)",
  "com.socrata" %% "socrata-zookeeper" % "0.1.2"
)

// TODO: enable static analysis build failures
com.socrata.sbtplugins.StylePlugin.StyleKeys.styleFailOnError in Compile := false
com.socrata.sbtplugins.findbugs.JavaFindBugsPlugin.JavaFindBugsKeys.findbugsFailOnError in Compile := false
com.socrata.sbtplugins.findbugs.JavaFindBugsPlugin.JavaFindBugsKeys.findbugsFailOnError in Test := false
