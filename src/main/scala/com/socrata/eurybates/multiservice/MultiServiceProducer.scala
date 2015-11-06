package com.socrata.eurybates.multiservice

import java.util.Properties

import com.socrata.eurybates.Producer.ProducerType
import com.socrata.eurybates.Producer.ProducerType.ProducerType
import com.socrata.eurybates._
import com.socrata.eurybates.activemq.ActiveMQServiceProducer
import com.socrata.eurybates.kafka.KafkaServiceProducer

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
      case i : String => throw new IllegalStateException("Unknown producer configured: " + i)
      case _ => throw new IllegalStateException("Unknown producer configured.")
    })
  }
}

case class MultiServiceProducer(sourceId:String, producers:List[Producer]) extends MessageCodec(sourceId) with Producer {

  override def send(message: Message) = producers.foreach(_.send(message))

  override def start() = synchronized {
    producers.foreach((producer) => producer.start())
  }

  override def stop() = synchronized {
    producers.foreach((producer) => producer.stop())
  }

  override def setServiceNames(serviceNames: Traversable[ServiceName]) : Unit = synchronized {
    producers.foreach((producer) => producer.setServiceNames(serviceNames))
  }

  override def supportedProducerTypes() = producers.flatMap(_.supportedProducerTypes)

  override def send(message: Message, producerType: ProducerType): Unit = {
    producers.foreach((producer) =>
      try {
        producer.send(message, producerType)
        return
      } catch {
        case iae: IllegalArgumentException =>
      }
    )

    throw new IllegalArgumentException("Could not publish to any of the publishers")
  }
}
