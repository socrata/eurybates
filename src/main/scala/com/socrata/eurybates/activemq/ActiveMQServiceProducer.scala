package com.socrata
package eurybates
package activemq

import java.util.Properties
import javax.jms.{Connection, DeliveryMode, JMSException, MessageProducer, Queue, Session}

import com.rojoma.json.v3.util.JsonUtil
import com.socrata.eurybates.Producer.ProducerType
import com.socrata.eurybates.Producer.ProducerType.ProducerType
import com.socrata.util.logging.LazyStringLogger
import org.apache.activemq.ActiveMQConnectionFactory

// technically, a Session is supposed to be used by only a single thread.  Fortunately, activemq
// is more lenient than strict JMS.

object ActiveMQServiceProducer {
  def apply(sourceId: String, properties: Properties): Producer = {
    properties.getProperty("eurybates." + ProducerType.ActiveMQ + ".connection_string") match {
      case conn: String => new ActiveMQServiceProducer(openActiveMQConnection(conn), sourceId, true, true)
      case _ => throw new IllegalStateException("No configuration passed for ActiveMQ")
    }
  }

  def openActiveMQConnection(amqUrl: String): Connection = {
    try {
      val connFactory: ActiveMQConnectionFactory = new ActiveMQConnectionFactory(amqUrl)
      val amqConnection = connFactory.createConnection
      amqConnection.start()
      amqConnection
    } catch {
      case e: JMSException =>
        throw new IllegalStateException("Unable to set up activemq connection", e)
    }
  }
}

case class ActiveMQServiceProducer(connection: Connection,
                                   sourceId: String,
                                   encodePrettily: Boolean = true,
                                   closeConnection: Boolean = false
                                  ) extends MessageCodec(sourceId) with Producer {

  private val log = new LazyStringLogger(getClass)

  var session: Option[Session] = None
  var producer: Option[MessageProducer] = None

  @volatile
  var queue: Option[Queue] = None

  def start(): Unit = synchronized {
    if (producer.isDefined) {
      throw new IllegalStateException("Producer is already started")
    } else {
      try {
        session = Option(connection.createSession(false, Session.AUTO_ACKNOWLEDGE))
        producer = session.map(_.createProducer(null)) // scalastyle:ignore null
        producer.foreach(_.setDeliveryMode(DeliveryMode.PERSISTENT))
      } catch {
        case e: Exception =>
          session.foreach(_.close())
          session = None
          throw e
      }
    }
  }

  def stop(): Unit = synchronized {
    producer match {
      case Some(someProducer) =>
        someProducer.close()
        producer = None
        session.foreach(_.close())
        session = None

        if (closeConnection) {
          connection.close()
        }
      case None => throw new IllegalStateException("Producer is already stopped")
    }
  }

  override def setServiceNames(serviceNames: Traversable[ServiceName]): Unit = {
    log.info("Setting service names to " + serviceNames + " for ActiveMQ")
    if (serviceNames.isEmpty) {
      queue = None
    } else {
      session match {
        case Some(someSession) =>
          val newNames = serviceNames.map(Name + "." + _).mkString(",")
          queue = Option(someSession.createQueue(newNames))
        case None => throw new IllegalStateException("Session has not been started")
      }
    }
  }

  def send(message: Message): Unit = {
    session match {
      case Some(someSession) =>
        log.trace("Sending " + message)
        val encodedMessage = JsonUtil.renderJson(message, pretty = encodePrettily)
        val qMessage = someSession.createTextMessage(encodedMessage)
        queue.foreach(target => producer.foreach(_.send(target, qMessage)))
      case None => throw new IllegalStateException("Session has not been started")
    }
  }

  def supportedProducerTypes(): Seq[ProducerType] = {
    Seq(ProducerType.ActiveMQ)
  }

  setServiceNames(Set.empty)
}
