package com.socrata.eurybates.check

import com.socrata.zookeeper.ZooKeeperProvider
import com.socrata.util.logging.LazyStringLogger
import com.socrata.eurybates._
import com.socrata.eurybates.kafka.{KafkaServiceConsumer, KafkaServiceProducer}
import com.socrata.eurybates.message.Envelope

// scalastyle:off magic.number

object KafkaCheck {
  val log = new LazyStringLogger(getClass)

  object First extends CheckMessage("first")
  object Second extends CheckMessage("second")

  class Spyer(underlying: Consumer) extends Spying(underlying) {
    def spy(message: Envelope): Unit = {
      log.info("Spied" + message.details)
    }
  }

  def greetConsumer(label: String): Consumer =
    new Spyer(
      Consumer.Builder.
        consuming[First.Message.type] { _ => log.info(label + " received first!") }.
        consuming[Second.Message.type] { _ => log.info(label + " received second!") }.
        build())

  def onUnexpectedException(sn: ServiceName, msgText: String, ex: Throwable): Unit = {
    log.error(sn + " received unknown message " + msgText, ex)
  }

  def main(args: Array[String]): Unit = {
    val executor = java.util.concurrent.Executors.newCachedThreadPool()

    val zkp = new ZooKeeperProvider("localhost:2181", 20000, executor)
    val producer = new KafkaServiceProducer("localhost:2181", "hello!", true)

    log.info("Staring producer service")
    producer.start()

    log.info("Starting consumers")
    val consumer = new KafkaServiceConsumer(
      "localhost:2181",
      "hello!",
      executor,
      onUnexpectedException,
      Map(
        "first" -> greetConsumer("a"),
        "second" -> greetConsumer("b")
      )
    )
    consumer.start()

    val firstMessageType = new CheckMessage("first")
    val secondMessageType = new CheckMessage("second")

    for {
      i <- 0 until 3
    } yield {
      producer.send(firstMessageType.Message)
      producer.send(secondMessageType.Message)
      Thread.sleep(10)
    }

    Thread.sleep(100)
    producer.stop()
    consumer.stop()
    executor.shutdown()
  }
}
