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
    val socrata_zookeeper = "1.0.0"
    val scala_test = "2.2.5"
    val slf4j = "1.7.21"
  }

  ////////////////////////////////////////////////////////////////////////////////
  // Library Dependencies for all sub projects
  ////////////////////////////////////////////////////////////////////////////////

  val activemq = "org.apache.activemq" % "activemq-core" % versions.activemq % "optional"
  val kafka_clients = "org.apache.kafka" % "kafka-clients" % versions.kafka_clients % "optional"
  val rojoma_json = "com.rojoma" %% "rojoma-json-v3" % versions.rojoma_json
  val socrata_zookeeper = "com.socrata" %% "socrata-zookeeper" % versions.socrata_zookeeper
  val scala_test = "org.scalatest" %% "scalatest" % versions.scala_test % "test"
  val slf4j = "org.slf4j" % "slf4j-api" % versions.slf4j

}
