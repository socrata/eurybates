package com.socrata.eurybates.check

import com.socrata.zookeeper.ZooKeeperProvider
import com.socrata.eurybates.zookeeper.ServiceConfiguration
import com.rojoma.json.ast._
import com.socrata.util.logging.LazyStringLogger
import com.socrata.eurybates._
import kafka.com.socrata.eurybates.kafka.KafkaServiceConsumer
import kafka.KafkaServiceProducer

object kafkaCheck {
  val log = new LazyStringLogger(getClass)

   class GreetConsumer(label: String) extends Consumer {
    val accepts = Set("first", "second")
    def consume(message: Message) { println(label + " received " + message) }
  }

  trait Spyer extends Spying {
    def spy(message:Message) = println("Spied" + message.details)
  }

  def greetService(label: String) = new SimpleService(List(new GreetConsumer(label) with Spyer))

  def onUnexpectedException(sn: ServiceName, msgText: String, ex: Throwable) {
    log.error(sn + " received unknown message " + msgText, ex)
  }

  def main(args: Array[String]) {
    val executor = java.util.concurrent.Executors.newCachedThreadPool()

    val zkp = new ZooKeeperProvider("localhost:2181", 20000, executor)
    val producer = new KafkaServiceProducer("localhost:2181", "hello!", true)

    log.info("Staring producer service")
    producer.start()

    log.info("Starting consumers")
    val consumer = new KafkaServiceConsumer("localhost:2181", "hello!", executor, onUnexpectedException,
      Map("first" -> greetService("a"),
          "second" -> greetService("b")))
    consumer.start()


    for(i <- 0 until 3) {
      producer(Message("first", JNull))
      producer(Message("second", JNull))
      Thread.sleep(10)
    }

    Thread.sleep(100)
    producer.stop()
    consumer.stop()
    executor.shutdown()
  }
}
