import sbt._

/**
  * Single reference to the entirety of Cassandra Scala Tutorial dependencies.
  */
object Dependencies {

  /**
    * Version numbers.
    */
  private object versions {
    val activemq = "5.7.0"
    val kafka_clients = "0.8.2.1"
    val rojoma_json = "[3.2.1,4.0.0]"
    val socrata_zookeeper = "0.1.4"
    val scala_test = "2.2.5"
    val scala_logging = "3.1.0"
  }

  object Resolvers {
    val socrata_maven = "socrata maven" at "https://repo.socrata.com/artifactory/libs-release"
    val socrata_ivy = Resolver.url("socrata ivy", new URL("https://repo.socrata.com/artifactory/ivy-libs-release"))(Resolver.ivyStylePatterns)
  }

  ////////////////////////////////////////////////////////////////////////////////
  // Library Dependencies for all sub projects
  ////////////////////////////////////////////////////////////////////////////////

  val activemq = "org.apache.activemq" % "activemq-core" % versions.activemq % "optional"
  val kafka_clients = "org.apache.kafka" % "kafka-clients" % versions.kafka_clients % "optional"
  val rojoma_json = "com.rojoma" %% "rojoma-json-v3" % versions.rojoma_json
  val socrata_zookeeper = "com.socrata" %% "socrata-zookeeper" % versions.socrata_zookeeper
  val scala_test = "org.scalatest" %% "scalatest" % versions.scala_test % "test"
  val scala_logging = "com.typesafe.scala-logging" %% "scala-logging" % versions.scala_logging

}