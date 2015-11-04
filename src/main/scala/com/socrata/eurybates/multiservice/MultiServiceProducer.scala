package com.socrata.eurybates.multiservice

import java.util.Properties

import com.socrata.eurybates.activemq.ActiveMQServiceProducer
import com.socrata.eurybates.kafka.KafkaServiceProducer
import com.socrata.eurybates._

/** A producer that sends to multiple sub-producers
 *
 * Startup and shutdown/cleanup operations are not considered part of the lifecycle of this. You should ensure that
 * your individual Producer objects are already initialized, and clean them up when you're done.
 */

object MultiServiceProducer {
  def fromProperties(sourceId: String, properties: Properties, producers: List[String]): Producer = {
    new MultiServiceProducer(sourceId, producers map {
      case Producer.ActiveMQProducerType => ActiveMQServiceProducer.fromProperties(sourceId, properties)
      case Producer.KafkaProducerType => KafkaServiceProducer.fromProperties(sourceId, properties)
      case Producer.NoopProducerType => new NoopProducer()
      case i : String => throw new IllegalStateException("Unknown producer configured: " + i)
      case _ => throw new IllegalStateException("Unknown producer configured.")
    })
  }
}

class MultiServiceProducer(sourceId:String, producers:List[Producer]) extends MessageCodec(sourceId) with Producer {
  def apply(message: Message) {
    for(producer <- producers) {
      producer.apply(message)
    }
  }

  def start() = synchronized {
    producers.foreach((producer) => producer.start())
  }

  def stop() = synchronized {
    producers.foreach((producer) => producer.stop())
  }

  override def setServiceNames(serviceNames: Traversable[ServiceName]) : Unit = synchronized {
    producers.foreach((producer) => producer.setServiceNames(serviceNames))
  }
}
