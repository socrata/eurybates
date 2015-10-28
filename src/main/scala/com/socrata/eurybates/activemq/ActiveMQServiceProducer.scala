package com.socrata
package eurybates
package activemq

import java.lang.IllegalStateException
import java.util.Properties
import util.logging.LazyStringLogger
import javax.jms.{Connection, Queue, MessageProducer, DeliveryMode, Session, JMSException}
import com.rojoma.json.v3.util.JsonUtil
import org.apache.activemq.ActiveMQConnectionFactory

// technically, a Session is supposed to be used by only a single thread.  Fortunately, activemq
// is more lenient than strict JMS.

object ActiveMQServiceProducer {
  def fromProperties(sourceId: String, properties: Properties) : Producer = {
    properties.getProperty(Producer.ActiveMQProducerType + "." + "connection_string") match {
      case conn: String => new ActiveMQServiceProducer(openActiveMQConnection(conn), sourceId )
      case _ => throw new IllegalStateException("No configuration passed for ActiveMQ")
    }
  }

  def openActiveMQConnection(amqUrl: String): Connection = {
    try {
      val connFactory: ActiveMQConnectionFactory = new ActiveMQConnectionFactory(amqUrl)
      val amqConnection = connFactory.createConnection
      amqConnection.start
      amqConnection
    } catch {
      case e: JMSException => {
        throw new IllegalStateException("Unable to set up activemq connection", e)
      }
    }
  }
}

class ActiveMQServiceProducer(connection: Connection, sourceId: String, encodePrettily: Boolean = true)
  extends MessageCodec(sourceId) with Producer {

  val log = new LazyStringLogger(getClass)

  var session: Session = _
  var producer: MessageProducer = _

  @volatile
  var queue: Queue = null

  def start() : Unit = synchronized {
    if(producer != null) throw new IllegalStateException("Producer is already started")
    try {
      session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
      producer = session.createProducer(null)
      producer.setDeliveryMode(DeliveryMode.PERSISTENT)
    } catch {
      case e: Exception =>
        if(session != null) session.close()
        session = null
        throw e
    }
  }

  def stop() : Unit = synchronized {
    if(producer == null) throw new IllegalStateException("Producer is already stopped")
    producer.close()
    producer = null
    session.close()
    session = null
  }

  override def setServiceNames(serviceNames: Traversable[ServiceName]) : Unit = {
    log.info("Setting service names to " + serviceNames + " for ActiveMQ")
    if(serviceNames.isEmpty) {
      queue = null
    } else {
      val newNames = serviceNames.map(Name + "." + _).mkString(",")
      queue = session.createQueue(newNames)
    }
  }

  def apply(message: Message) : Unit = {
    log.trace("Sending " + message)
    val encodedMessage = JsonUtil.renderJson(message, pretty = encodePrettily)
    val qMessage = session.createTextMessage(encodedMessage)
    val target = queue
    if(target != null) producer.send(target, qMessage)
  }

  setServiceNames(Set.empty)
}
