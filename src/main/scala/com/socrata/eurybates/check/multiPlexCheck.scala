package com.socrata.eurybates.check

import com.socrata.zookeeper.ZooKeeperProvider
import com.socrata.eurybates.zookeeper.ServiceConfiguration
import com.rojoma.json.v3.ast.JNull
import com.socrata.util.logging.LazyStringLogger
import com.socrata.eurybates._
import com.socrata.eurybates.kafka.KafkaServiceConsumer
import com.socrata.eurybates.activemq.{ActiveMQServiceProducer, ActiveMQServiceConsumer}
import com.socrata.eurybates.multiplexer.MultiServiceProducer

import kafka.KafkaServiceProducer


object multiPlexCheck {
  val log = new LazyStringLogger(getClass)

  def greetConsumerKafka(label: String) = new Consumer {
    val accepts = Set("first", "second")

    def consume(message: Message) {
      println("Kafka" + label + " received " + message)
    }
  }

  def greetServiceKafka(label: String) = new SimpleService(List(greetConsumerKafka(label)))



  def onUnexpectedException(sn: ServiceName, msgText: String, ex: Throwable) {
    log.error(sn + " received unknown message " + msgText, ex)
  }

  def greetConsumerAMQP(label: String) = new Consumer {
    val accepts = Set("hello","first","second")
    def consume(message: Message) { println("AMQP" + label + " received " + message) }
  }

  def greetService(label: String) = new SimpleService(List(greetConsumerAMQP(label),greetConsumerKafka(label)))



  def main(args: Array[String]) {
    val executor = java.util.concurrent.Executors.newCachedThreadPool()

    val zkp = new ZooKeeperProvider("localhost:2181", 20000, executor)
    val connFactory = new org.apache.activemq.ActiveMQConnectionFactory("failover:(tcp://localhost:61616)")

    val conn = connFactory.createConnection()
    conn.start()



    val producerAMQP = new ActiveMQServiceProducer(conn, "hello!", true)
    producerAMQP.start()

    val config = new ServiceConfiguration(zkp, executor, producerAMQP.setServiceNames)
    config.start().foreach(config.destroyService)

    config.createService("first")
    config.createService("second")

    Thread.sleep(100)
    val producerKafka = new KafkaServiceProducer("localhost:2181", "hello!", true)

    log.info("Staring producer service")
    producerKafka.start()

    val multiplexer = new MultiServiceProducer("multi", List(producerAMQP,producerKafka))


    log.info("Starting consumers")
    val consumer = new KafkaServiceConsumer("localhost:2181", "hello!", executor, onUnexpectedException,
      Map(
        "second" -> greetService("b")))
    consumer.start()

    val consumerampq = new ActiveMQServiceConsumer(conn, "hello!", executor, onUnexpectedException,
                           Map("first" -> greetService("a"), "second" -> greetService("b")))
    consumerampq.start()


    for (i <- 0 until 100) {
      multiplexer(Message("first", JNull))
      multiplexer(Message("second", JNull))
      Thread.sleep(100)
    }

    Thread.sleep(1000)
    producerAMQP.stop()
    producerKafka.stop()
    consumer.stop()
    consumerampq.stop()
    conn.close()
    executor.shutdown()
  }



}
