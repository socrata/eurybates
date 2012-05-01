package com.socrata
package eurybates.kafka
package com.socrata.eurybates.kafka

import scala.util.control.{Exception => ExceptionUtil}
import kafka.message.{Message => KafkaMessage}
import java.util.concurrent.ExecutorService
import java.util.Properties
import _root_.kafka.utils.Utils
import _root_.com.rojoma.json.io.JsonReaderException
import _root_.com.rojoma.json.util.JsonUtil._
import util.logging.LazyStringLogger
import kafka.consumer.{Consumer, ConsumerConfig, ConsumerConnector}
import eurybates.{MessageCodec, ServiceName, Service, Message}

class KafkaServiceConsumer(zookeeperServers: String, sourceId: String, executor: ExecutorService, handlingLogger: (ServiceName, String, Throwable) => Unit, services: Map[ServiceName, Service])
  extends MessageCodec(sourceId)
{
  val log = new LazyStringLogger(getClass)
  private var consumerConnector : ConsumerConnector = null
  
  def start() = synchronized {
    val props = new Properties
    props.put("zk.connect", zookeeperServers)
    props.put("groupid", sourceId)
    val consumerConfig = new ConsumerConfig(props)
    consumerConnector = Consumer.create(consumerConfig)
    val topicMessageStreams = consumerConnector.createMessageStreams(services map { kv => ("eurybates." + kv._1, 1)})
    
    services.map {
      case (serviceName, service) =>
        val streams = topicMessageStreams.apply("eurybates." + serviceName)
        for(stream <- streams) {
          executor.execute(new Runnable() {
            def run() {
              for(message:KafkaMessage <- stream) {
                val textMessage = Utils.toString(message.payload, "UTF-8")
                ExceptionUtil.catching(classOf[JsonReaderException]).either(parseJson[Message](textMessage)) match {
                  case Right(Some(msg:Message)) =>
                    service.messageReceived(msg)
                  case Right(None) =>
                    log.warn("Unable to parse JSON as a Message object: " + textMessage)
                  case Left(exn) =>
                    log.warn("Received a non-JSON text message: " + textMessage, exn)
                }
              }
            }
          })
      }
    }
  }

  def stop() = synchronized {
    consumerConnector.shutdown
    executor.shutdownNow
  }

  def close() = synchronized {

  }
}
