package com.socrata.eurybates

import java.util.Properties

import com.socrata.eurybates
import com.socrata.eurybates.activemq.ActiveMQServiceProducer
import com.socrata.eurybates.kafka.KafkaServiceProducer
import com.socrata.eurybates.multiservice.MultiServiceProducer
import com.socrata.eurybates.zookeeper.ServiceConfiguration
import com.socrata.util.logging.LazyStringLogger

/** A Producer accepts messages from user code and routes them to a topic.
  *
  * In the ActiveMQ implementation, this is sent to all services based on a registry maintained
  * in ZooKeeper, and the consumer code is responsible for filtering out which messages it wants.
  *
  * In the Kafka implementation, each Message is sent to a specific topic. The consumer code only
  * receives messages on the topic(s) they wish to subscribe to.
  */

object Producer {
  final val KafkaProducerType = "kafka"
  final val ActiveMQProducerType = "activemq"
  final val NoopProducerType = "null"

  def fromProperties(sourceId: String, properties: Properties) : Producer = {
    properties.getProperty("producers") match {
      case KafkaProducerType => KafkaServiceProducer.fromProperties(sourceId, properties)
      case ActiveMQProducerType => ActiveMQServiceProducer.fromProperties(sourceId, properties)
      case NoopProducerType => new NoopProducer()
      case i: String if i.isEmpty => throw new IllegalStateException("No producers configured")
      case i: String => MultiServiceProducer.fromProperties(sourceId, properties, i.split(',').toList)
      case _ => throw new IllegalStateException("No producers configured")
    }
  }
}

trait Producer {
  private val log = new LazyStringLogger(getClass)

  def start()

  def stop()

  def apply(message: eurybates.Message)

  def setServiceNames(serviceNames: Traversable[ServiceName]) : Unit = {
    //Default is noop
    log.info("No Op : Not Setting service names")
  }

  def jSetServiceNames : Function1[Set[ServiceName], Unit] = setServiceNames _
}
