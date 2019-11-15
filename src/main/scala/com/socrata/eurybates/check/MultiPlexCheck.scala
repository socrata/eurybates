package com.socrata.eurybates.check

import com.socrata.zookeeper.ZooKeeperProvider
import com.socrata.eurybates.zookeeper.ServiceConfiguration
import com.socrata.util.logging.LazyStringLogger
import com.socrata.eurybates._
import com.socrata.eurybates.kafka.KafkaServiceConsumer
import com.socrata.eurybates.activemq.{ActiveMQServiceProducer, ActiveMQServiceConsumer}
import com.socrata.eurybates.multiservice.MultiServiceProducer

import kafka.KafkaServiceProducer

// scalastyle:off magic.number
// scalastyle:off multiple.string.literals

object MultiPlexCheck {
  val log = new LazyStringLogger(getClass)

  object First extends CheckMessage("first")
  object Second extends CheckMessage("second")

  def greetConsumerKafka(label: String): Consumer =
    Consumer.Builder.
      consuming[First.Message.type] { _ => log.info("Kafka " + label + " received first!") }.
      consuming[Second.Message.type] { _ => log.info("Kafka " + label + " received second!") }.
      build()

  def onUnexpectedException(sn: ServiceName, msgText: String, ex: Throwable): Unit = {
    log.error(sn + " received unknown message " + msgText, ex)
  }

  def greetConsumerAMQP(label: String): Consumer =
    Consumer.Builder.
      consuming[First.Message.type] { _ => log.info("AMQP " + label + " received first!") }.
      consuming[Second.Message.type] { _ => log.info("AMQP " + label + " received second!") }.
      consuming[CheckMessage.Hello.Message.type] { _ => log.info("AMQP " + label + " received hello!") }.
      build()

  def main(args: Array[String]): Unit = {
    val executor = java.util.concurrent.Executors.newCachedThreadPool()

    val zkp = new ZooKeeperProvider("localhost:2181", 20000, executor)
    val connFactory = new org.apache.activemq.ActiveMQConnectionFactory("failover:(tcp://localhost:61616)")

    val conn = connFactory.createConnection()
    conn.start()

    val producerAMQP = new ActiveMQServiceProducer(conn, "hello!", true)
    producerAMQP.start()

    val config = new ServiceConfiguration(zkp, executor, producerAMQP.setServiceNames)
    config.start().foreach(config.destroyService)
    config.registerService("first")
    config.registerService("second")

    Thread.sleep(100)
    val producerKafka = new KafkaServiceProducer("localhost:2181", "hello!", true)

    log.info("Staring producer service")
    producerKafka.start()

    val multiplexer = new MultiServiceProducer("multi", List(producerAMQP,producerKafka))


    log.info("Starting consumers")
    val consumer = new KafkaServiceConsumer("localhost:2181", "hello!", executor, onUnexpectedException,
      Map(
        "second" -> greetConsumerKafka("b")))
    consumer.start()

    val consumerampq = new ActiveMQServiceConsumer(conn, "hello!", executor, onUnexpectedException,
                           Map("first" -> greetConsumerAMQP("a"), "second" -> greetConsumerAMQP("b")))
    consumerampq.start()

    (0 until 100).foreach(_ => {
      multiplexer.send(First.Message)
      multiplexer.send(Second.Message)
      Thread.sleep(100)
    })

    Thread.sleep(1000)
    producerAMQP.stop()
    producerKafka.stop()
    consumer.stop()
    consumerampq.stop()
    conn.close()
    executor.shutdown()
  }
}
