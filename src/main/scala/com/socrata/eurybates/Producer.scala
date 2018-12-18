package com.socrata.eurybates

import java.util.Properties

import com.socrata.eurybates
import com.socrata.eurybates.Producer.ProducerType.ProducerType
import com.socrata.eurybates.activemq.ActiveMQServiceProducer
import com.socrata.eurybates.kafka.KafkaServiceProducer
import com.socrata.eurybates.multiservice.MultiServiceProducer
import org.slf4j.LoggerFactory

/** A Producer accepts messages from user code and routes them to a topic.
  *
  * In the ActiveMQ implementation, this is sent to all services based on a registry maintained
  * in ZooKeeper, and the consumer code is responsible for filtering out which messages it wants.
  *
  * In the Kafka implementation, each Message is sent to a specific topic. The consumer code only
  * receives messages on the topic(s) they wish to subscribe to.
  */

object Producer {
  object ProducerType extends Enumeration{
    type ProducerType = String
    val ActiveMQ = "activemq"
    val Kafka = "kafka"
    val LocalService = "local_service"
    val NoOp = "noop"
  }

  def apply(sourceId: String, properties: Properties) : Producer = {
    properties.getProperty("eurybates.producers") match {
      case ProducerType.Kafka => KafkaServiceProducer(sourceId, properties)
      case ProducerType.ActiveMQ => ActiveMQServiceProducer(sourceId, properties)
      case ProducerType.NoOp => new NoopProducer(sourceId)
      case i: String if i.isEmpty => throw new IllegalStateException("No producers configured")
      case i: String => MultiServiceProducer(sourceId, properties, i.split(',').toList)
      case _ => throw new IllegalStateException("No producers configured")
    }
  }
}

trait Producer {
  private val log = LoggerFactory.getLogger(classOf[Producer])

  def start(): Unit

  def stop(): Unit

  def supportedProducerTypes() : Seq[ProducerType]

  def send(message: eurybates.Message): Unit

  def send(message: eurybates.Message, producerType: ProducerType) : Unit = {
    if(supportedProducerTypes().contains(producerType)){
      throw new IllegalArgumentException("Trying to send unsupported producer type for this producer")
    }

    send(message)
  }

  def setServiceNames(serviceNames: Traversable[ServiceName]) : Unit = {
    // Default is noop
    log.info("No Op : Not Setting service names")
  }

  def jSetServiceNames : Function1[Set[ServiceName], Unit] = setServiceNames _

}
