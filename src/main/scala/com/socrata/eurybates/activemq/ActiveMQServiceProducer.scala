package com.socrata
package eurybates
package activemq

import com.rojoma.json.util.JsonUtil._
import java.lang.IllegalStateException
import util.logging.LazyStringLogger
import javax.jms.{Connection, Queue, MessageProducer, DeliveryMode, Session}

// technically, a Session is supposed to be used by only a single thread.  Fortunately, activemq
// is more lenient than strict JMS.
class ActiveMQServiceProducer(connection: Connection, sourceId: String, encodePrettily: Boolean) extends MessageCodec(sourceId) with Producer {
  val log = new LazyStringLogger(getClass)

  var session: Session = _
  var producer: MessageProducer = _

  @volatile
  var queue: Queue = null

  def start() = synchronized {
    if(producer != null) throw new IllegalStateException("Producer is already started")
    try {
      session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
      producer = session.createProducer(null)
      producer.setDeliveryMode(DeliveryMode.PERSISTENT)
    } catch {
      case e: Exception =>
        if(session != null) session.close();
        session = null
        throw e
    }
  }

  def stop() = synchronized {
    if(producer == null) throw new IllegalStateException("Producer is already stopped")
    producer.close()
    producer = null
    session.close()
    session = null
  }

  def setServiceNames(serviceNames: Traversable[ServiceName]) {
    log.info("Setting service names to " + serviceNames)
    if(serviceNames.isEmpty) queue = null
    else {
      val newNames = serviceNames.map("eurybates." + _).mkString(",")
      queue = session.createQueue(newNames)
    }
  }

  def jSetServiceNames : Function1[Set[ServiceName], Unit] = setServiceNames _

  def apply(message: Message) {
    log.trace("Sending " + message)
    val encodedMessage = renderJson(message, pretty = encodePrettily)
    val qMessage = session.createTextMessage(encodedMessage)
    val target = queue
    if(target != null) producer.send(target, qMessage)
  }

  setServiceNames(Set.empty)
}
