package com.socrata
package eurybates
package kafka

import java.nio.charset.{StandardCharsets}
import java.util.concurrent.ExecutorService
import java.util.Properties
import com.socrata.eurybates.Producer.ProducerType
import util.logging.LazyStringLogger
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord, KafkaConsumer, Consumer}
import eurybates.{MessageCodec, ServiceName, Service, Message}

import com.rojoma.json.v3.io.JsonReaderException
import com.rojoma.json.v3.util.JsonUtil
import com.rojoma.json.v3.codec.DecodeError.InvalidValue
import com.rojoma.json.v3.ast.JString
import com.rojoma.json.v3.codec.Path
import scala.annotation.tailrec
import scala.collection.JavaConverters._

class KafkaServiceConsumer(brokerList: String, sourceId: String, executor: ExecutorService, handlingLogger: (ServiceName, String, Throwable) => Unit, services: Map[ServiceName, Service])
  extends MessageCodec(sourceId)
{
  val log = new LazyStringLogger(getClass)

  var consumers: Iterable[Consumer[Array[Byte],Array[Byte]]] = Seq[Consumer[Array[Byte],Array[Byte]]]()

  def start() = synchronized {
    log.info("Starting kafka consumer with brokers: " + brokerList)

    val props = new Properties
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList)
    props.put(ConsumerConfig.SESSION_TIMEOUT_MS, "1000")
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "10000")

    consumers = services.map {
      case (serviceName, service) =>
        val serviceProps = new Properties(props)
        serviceProps.put(ConsumerConfig.GROUP_ID_CONFIG, serviceName)

        val consumer = new KafkaConsumer[Array[Byte],Array[Byte]](serviceProps)

        consumer.subscribe(Name)

        executor.execute(new Runnable() {
          def run(): Unit = {
            log.info("Starting polling thread for " + serviceName)
            pollAndProcess(consumer, service)()
          }
        })

        consumer
    }
  }

  def pollAndProcess(consumer: Consumer[Array[Byte],Array[Byte]], service: Service) = {
    new (() => Unit) {
      @tailrec
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
    log.info("Stopping consumers and threads")
    for(consumer: Consumer[Array[Byte],Array[Byte]] <- consumers) {
      consumer.close()
    }

    executor.shutdownNow
    log.info("Successfully stopped consumers and threads")
  }

  def close() = synchronized {

  }

  def supportedProducerTypes() = {
    Seq(ProducerType.Kafka)
  }
}