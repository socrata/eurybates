package com.socrata.eurybates.check

import com.socrata.eurybates.activemq.{ActiveMQServiceProducer, ActiveMQServiceConsumer}
import com.socrata.zookeeper.ZooKeeperProvider
import com.socrata.eurybates.zookeeper.ServiceConfiguration
import com.socrata.util.logging.LazyStringLogger
import com.socrata.eurybates._

// scalastyle:off magic.number

object Check {
  val log = new LazyStringLogger(getClass)

  def greetConsumer(label: String): Consumer =
    Consumer.Builder.
      consuming[CheckMessage.Hello.Message.type] { _ => log.info(label + " received hello") }.
      build()

  def greetService(label: String): SimpleService =
    new SimpleService(List(greetConsumer(label)))

  def onUnexpectedException(sn: ServiceName, msgText: String, ex: Throwable): Unit = {
    log.error(sn + " received unknown message " + msgText, ex)
  }

  def main(args: Array[String]): Unit = {
    val executor = java.util.concurrent.Executors.newCachedThreadPool()

    val zkp = new ZooKeeperProvider("mike.local:2181", 20000, executor)
    val connFactory = new org.apache.activemq.ActiveMQConnectionFactory("failover:(tcp://mike.local:61616)")
    val conn = connFactory.createConnection()
    conn.start()

    val producer = new ActiveMQServiceProducer(conn, "hello!", true)
    producer.start()

    val config = new ServiceConfiguration(zkp, executor, producer.setServiceNames)
    config.start().foreach(config.destroyService)
    // producer.setServiceNames(config.start())

    val consumer = new ActiveMQServiceConsumer(
      conn,
      "hello!",
      executor,
      onUnexpectedException,
      Map(
        "first" -> greetService("a"),
        "second" -> greetService("b")
      )
    )
    consumer.start()

    for {
      i <- 0 until 100
    } yield {
      producer.send(CheckMessage.Hello.Message)
      if (i == 30) {
        config.registerService("first")
      } else if (i == 60) {
        config.registerService("second")
      }
      Thread.sleep(100)
    }

    consumer.stop()
    producer.stop()
    conn.close()
    executor.shutdown()
  }
}
