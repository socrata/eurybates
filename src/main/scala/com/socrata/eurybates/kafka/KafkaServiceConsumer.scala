package com.socrata
package eurybates
package kafka

import java.util.concurrent.ExecutorService
import java.util.Properties
import util.logging.LazyStringLogger
import _root_.kafka.consumer.{Consumer, ConsumerConfig, ConsumerConnector}
import eurybates.{MessageCodec, ServiceName, Service, Message}

import com.rojoma.json.v3.io.JsonReaderException
import com.rojoma.json.v3.util.JsonUtil
import com.rojoma.json.v3.codec.DecodeError.InvalidValue
import com.rojoma.json.v3.ast.JString
import com.rojoma.json.v3.codec.Path

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
              for(messageMetadata <- stream) {
                val message = new String(messageMetadata.message, "UTF-8")
                val msg = try {
                  JsonUtil.parseJson[Message](message)
                } catch {
                  case _: JsonReaderException =>
                    Left(InvalidValue(JString(message), Path("details")))
                }
                msg match {
                  case Right(m) =>
                    service.messageReceived(m)
                  case Left(err) =>
                    log.warn("Received a non-JSON text message: " + err)
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