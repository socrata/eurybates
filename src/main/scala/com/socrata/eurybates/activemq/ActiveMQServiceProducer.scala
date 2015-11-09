package com.socrata
package eurybates
package activemq

import java.util.Properties
import javax.jms.{Connection, DeliveryMode, JMSException, MessageProducer, Queue, Session}

import com.rojoma.json.v3.util.JsonUtil
import com.socrata.eurybates.Producer.ProducerType
import com.socrata.util.logging.LazyStringLogger
import org.apache.activemq.ActiveMQConnectionFactory

// technically, a Session is supposed to be used by only a single thread.  Fortunately, activemq
// is more lenient than strict JMS.

object ActiveMQServiceProducer {
  def apply(sourceId: String, properties: Properties) : Producer = {
    properties.getProperty("eurybates." + ProducerType.ActiveMQ + ".connection_string") match {
      case conn: String => new ActiveMQServiceProducer(openActiveMQConnection(conn), sourceId, true, true )
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

case class ActiveMQServiceProducer(connection: Connection, sourceId: String, encodePrettily: Boolean = true, closeConnection: Boolean = false)
  extends MessageCodec(sourceId) with Producer {

  private val log = new LazyStringLogger(getClass)

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

    if(closeConnection){
      connection.close()
    }
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

  def send(message: Message) : Unit = {
    log.trace("Sending " + message)
    val encodedMessage = JsonUtil.renderJson(message, pretty = encodePrettily)
    val qMessage = session.createTextMessage(encodedMessage)
    val target = queue
    if(target != null) producer.send(target, qMessage)
  }

  def supportedProducerTypes() = {
    Seq(ProducerType.ActiveMQ)
  }

  setServiceNames(Set.empty)
}
