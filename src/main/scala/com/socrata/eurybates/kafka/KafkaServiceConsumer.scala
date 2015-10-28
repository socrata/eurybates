package com.socrata
package eurybates
package kafka

import java.nio.charset.{StandardCharsets}
import java.util.concurrent.ExecutorService
import java.util.Properties
import util.logging.LazyStringLogger
import org.apache.kafka.clients.consumer.{ConsumerRecord, KafkaConsumer, Consumer}
import eurybates.{MessageCodec, ServiceName, Service, Message}

import com.rojoma.json.v3.io.JsonReaderException
import com.rojoma.json.v3.util.JsonUtil
import com.rojoma.json.v3.codec.DecodeError.InvalidValue
import com.rojoma.json.v3.ast.JString
import com.rojoma.json.v3.codec.Path
import scala.collection.JavaConverters._

class KafkaServiceConsumer(brokerList: String, sourceId: String, executor: ExecutorService, handlingLogger: (ServiceName, String, Throwable) => Unit, services: Map[ServiceName, Service])
  extends MessageCodec(sourceId)
{
  val log = new LazyStringLogger(getClass)

  var consumers: Iterable[Consumer[Array[Byte],Array[Byte]]] = Seq[Consumer[Array[Byte],Array[Byte]]]()

  def start() = synchronized {
    val props = new Properties
    props.put("metadata.broker.list", brokerList)
    props.put("session.timeout.ms", "1000")
    props.put("enable.auto.commit", "true")
    props.put("auto.commit.interval.ms", "10000")

    consumers = services.map {
      case (serviceName, service) =>
        val serviceProps = new Properties(props)
        serviceProps.put("group.id", serviceName)

        val consumer = new KafkaConsumer[Array[Byte],Array[Byte]](serviceProps)

        executor.execute(new Runnable() {
          def run(): Unit = {
            pollAndProcess(consumer, service)()
          }
        })

        consumer
    }
  }

  def pollAndProcess(consumer: Consumer[Array[Byte],Array[Byte]], service: Service) = {
    new (() => Unit) {
      def apply(): Unit = {
        val polled = consumer.poll(0)

        val eurybatesMessages = polled.get(Name)

        for (record: ConsumerRecord[Array[Byte],Array[Byte]] <- eurybatesMessages.records().asScala.toList) {
          val message = new String(record.value(), StandardCharsets.UTF_8)

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

        apply()
      }

  }
}

  def stop() = synchronized {
    for(consumer: Consumer[Array[Byte],Array[Byte]] <- consumers) {
      consumer.close()
    }

    executor.shutdownNow
  }

  def close() = synchronized {

  }
}