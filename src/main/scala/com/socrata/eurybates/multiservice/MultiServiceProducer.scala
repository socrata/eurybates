package com.socrata.eurybates.multiservice

import java.util.Properties

import com.socrata.eurybates.Producer.ProducerType
import com.socrata.eurybates.Producer.ProducerType.ProducerType
import com.socrata.eurybates._
import com.socrata.eurybates.activemq.ActiveMQServiceProducer
import com.socrata.eurybates.kafka.KafkaServiceProducer

import scala.annotation.tailrec

/** A producer that sends to multiple sub-producers
  *
  * Startup and shutdown/cleanup operations are not considered part of the lifecycle of this. You should ensure that
  * your individual Producer objects are already initialized, and clean them up when you're done.
  */

object MultiServiceProducer {
  def apply(sourceId: String, properties: Properties, producers: List[String]): Producer = {
    new MultiServiceProducer(sourceId, producers map {
      case ProducerType.ActiveMQ => ActiveMQServiceProducer(sourceId, properties)
      case ProducerType.Kafka => KafkaServiceProducer(sourceId, properties)
      case ProducerType.NoOp => new NoopProducer(sourceId)
      case i: String => throw new IllegalStateException("Unknown producer configured: " + i)
      case _ => throw new IllegalStateException("Unknown producer configured.")
    })
  }
}

case class MultiServiceProducer(sourceId: String, producers: List[Producer])
  extends MessageCodec(sourceId) with Producer {

  override def send(message: Message): Unit = producers.foreach(_.send(message))

  override def start(): Unit = synchronized {
    producers.foreach((producer) => producer.start())
  }

  override def stop(): Unit = synchronized {
    producers.foreach((producer) => producer.stop())
  }

  override def setServiceNames(serviceNames: Traversable[ServiceName]): Unit = synchronized {
    producers.foreach((producer) => producer.setServiceNames(serviceNames))
  }

  override def supportedProducerTypes(): Seq[ProducerType] =
    producers.flatMap(_.supportedProducerTypes())

  override def send(message: Message, producerType: ProducerType): Unit = {
    sendHelper(message, producerType, producers)
  }

  @tailrec private def sendHelper(message: Message,
                                  producerType: ProducerType,
                                  producers: List[Producer]): Unit = {
    producers match {
      case producer :: tail =>
        try {
          producer.send(message, producerType)
        } catch {
          case iae: IllegalArgumentException =>
            sendHelper(message, producerType, tail)
        }
      case Nil =>
        throw new IllegalArgumentException("Could not publish to any of the publishers")
    }
  }
}
