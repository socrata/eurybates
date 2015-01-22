import com.socrata.socratasbt.SocrataSbt._
import SocrataSbtKeys._

seq(socrataSettings(): _*)

name := "eurybates"

scalaVersion := "2.10.4"

libraryDependencies <++= (slf4jVersion) { slf4jVersion =>
  Seq(
    "org.apache.activemq" % "activemq-core" % "5.7.0" % "optional",
    "org.apache.kafka" %% "kafka" % "0.8.1" % "optional",
    "com.rojoma" %% "rojoma-json-v3" % "[3.2.1,4.0.0)",
    "com.socrata" %% "socrata-zookeeper" % "0.1.2"
  )
}
