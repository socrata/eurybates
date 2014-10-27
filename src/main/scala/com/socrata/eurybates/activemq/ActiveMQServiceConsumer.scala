package com.socrata
package eurybates
package activemq

import java.util.concurrent.ExecutorService
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException

import scala.util.control.{Exception => ExceptionUtil}

import javax.jms.{JMSException, Connection, Session, TextMessage}
import util.logging.LazyStringLogger
import com.rojoma.json.v3.io.JsonReaderException
import com.rojoma.json.v3.util.JsonUtil
import com.rojoma.json.v3.codec.DecodeError.InvalidValue
import com.rojoma.json.v3.ast.JString
import com.rojoma.json.v3.codec.Path

class ActiveMQServiceConsumer(connection: Connection, sourceId: String, executor: ExecutorService, handlingLogger: (ServiceName, String, Throwable) => Unit, services: Map[ServiceName, Service]) extends MessageCodec(sourceId) with QueueUtil {
  val log = new LazyStringLogger(getClass)

  private val workers = services map { case (serviceName, service) => new ServiceProcess(serviceName, service) }

  def start() = synchronized {
    workers.foreach(_.start())
  }

  def stop() = synchronized {
    workers.foreach(_.close())
    workers.foreach(_.join())
  }

  private class ServiceProcess(serviceName: ServiceName, service: Service) extends Thread {
    var session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
    val queue = session.createQueue(queueName(serviceName))
    val consumer = session.createConsumer(queue)

    setName(getId() + " / Eurybates activemq service " + serviceName)

    private def nextMessage(): javax.jms.Message = {
      var sleepTime = 10L
      val sleepMax = 10000L
      while(true) {
        try {
          return consumer.receive()
        } catch {
          case _: javax.jms.IllegalStateException =>
            null // it was closed on this side
          case e: JMSException => // hmmmmm
            log.error("Unexpected JMS exception; sleeping and retrying", e)
            Thread.sleep(sleepTime)
            sleepTime = sleepMax.max(sleepTime * 2)
        }
      }
      sys.error("can't get here")
    }
    override def run() {
      try {
        while(true) {
          val qMsg = nextMessage()
          if(qMsg == null) return

          // Why, you may ask, are we using a thread pool to handle an
          // event only to immediately block and wait for its result?
          // The answer: so that this thread may be interrupted with a
          // guarantee that this loop will terminate.

          val result = executor.submit(new Callable[Unit] {
            def call() {
              qMsg match {
                case tm: TextMessage =>
                  val msg = try {
                    JsonUtil.parseJson[Message](tm.getText)
                  } catch {
                    case _: JsonReaderException =>
                      Left(InvalidValue(JString(tm.getText), Path("details")))
                  }
                  msg match {
                    case Right(m) =>
                      service.messageReceived(m)
                    case Left(err) =>
                      log.warn("Received a non-JSON text message: " + err)
                  }
                case _ =>
                  log.warn("Received a non-TextMessage from JMS; actual type received is " + qMsg.getClass)
              }
            }
          })
          try {
            result.get()
          } catch {
            case e: InterruptedException =>
              // don't cancel it; we've already acknowledged the message
              // so let the executor finish handling it.
              throw e
            case e: ExecutionException =>
              if(qMsg.isInstanceOf[TextMessage]) {
                handlingLogger(serviceName, qMsg.asInstanceOf[TextMessage].getText, e.getCause)
              } else {
                log.error("Received an unexpected exception while processing an instance of " + qMsg.getClass, e.getCause)
              }
          }
        }
      } catch {
        case e: InterruptedException => // time to go
      }
    }

    def close() = synchronized {
      consumer.close()
      session.close()
    }
  }
}
