package com.socrata
package eurybates.kafka

import com.rojoma.json.v3.util.JsonUtil
import com.socrata.eurybates.Producer.ProducerType
import util.logging.LazyStringLogger
import java.util.Properties

import com.socrata.eurybates.Producer.ProducerType.ProducerType
import com.socrata.eurybates._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}

object KafkaServiceProducer {
  def apply(sourceId: String, properties: Properties): Producer = {
    properties.getProperty("eurybates." + ProducerType.Kafka + "." + "broker_list") match {
      case brokerList: String => new KafkaServiceProducer(brokerList, sourceId)
      case _ => throw new IllegalStateException("No configuration passed for Kafka")
    }
  }
}

/** Submit Messages to a Kafka-based queue
  *
  * @param brokerList     Comma-separated list of host:port pairs representing Kafka brokers
  * @param sourceId       String representation of what created this. Auto-populated in messages.
  * @param encodePrettily Pretty-print JSON
  */
case class KafkaServiceProducer(brokerList: String,
                                sourceId: String,
                                encodePrettily: Boolean = true
                               ) extends MessageCodec(sourceId) with Producer with QueueUtil {
  val log = new LazyStringLogger(getClass)
  var producer: Option[KafkaProducer[String, String]] = None

  def start(): Unit = synchronized {
    val props = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList)
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    props.put(ProducerConfig.ACKS_CONFIG, "-1")

    producer = Some(new KafkaProducer(props))
  }

  def stop(): Unit = synchronized {
    producer.foreach(_.close())
  }

  def send(message: eurybates.Message): Unit = {
    producer match {
      case Some(someProducer) =>
        val queueName = Name
        val encodedMessage = JsonUtil.renderJson(message, pretty = encodePrettily)
        log.info("Sending " + message + " on queue " + queueName + "with tag " + message.tag)
        someProducer.send(new ProducerRecord[String, String](queueName, encodedMessage)).get()
      case None =>
        throw new IllegalStateException("Producer not initialized -- call 'start' first")
    }
  }

  def supportedProducerTypes(): Seq[ProducerType] = {
    Seq(ProducerType.Kafka)
  }
}
