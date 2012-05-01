package com.socrata
package eurybates.kafka

import util.logging.LazyStringLogger
import _root_.com.rojoma.json.util.JsonUtil._
import java.util.Properties
import eurybates.{MessageCodec, QueueUtil, Producer}
import kafka.producer.{ProducerData, Producer => KafkaProducer, ProducerConfig}

/** Submit Messages to a Kafka-based queue
 *
 * @param zookeeperServers Comma-separated list of host:port pairs representing ZooKeeper coordination nodes
 * @param sourceId String representation of what created this. Auto-populated in messages.
 * @param encodePrettily Pretty-print JSON
 */
class KafkaServiceProducer(zookeeperServers: String, sourceId:String, encodePrettily: Boolean) extends MessageCodec(sourceId) with Producer with QueueUtil {
  val log = new LazyStringLogger(getClass)
  var producer:KafkaProducer[String,  String] = null
  
  def start() = synchronized {
    var props = new Properties()
    props.put("zk.connect", zookeeperServers)
    props.put("serializer.class", "kafka.serializer.StringEncoder")
    val config = new ProducerConfig(props)
    producer = new kafka.producer.Producer[String, String](config)
  }

  def stop() = synchronized {
    producer.close()
  }

  def apply(message: eurybates.Message) {
    val queueName = "eurybates." + message.tag
    val encodedMessage = renderJson(message, pretty = encodePrettily)

    log.info("Sending " + message + " on queue " + queueName)
    producer.send(new ProducerData[String, String](queueName, encodedMessage))
  }
}
