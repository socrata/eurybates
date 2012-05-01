package com.socrata
package eurybates
package activemq

import java.util.concurrent.ExecutorService
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException

import scala.util.control.{Exception => ExceptionUtil}

import com.rojoma.json.util.JsonUtil._
import javax.jms.{JMSException, Connection, Session, TextMessage}
import util.logging.LazyStringLogger
import com.rojoma.json.io.JsonReaderException

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
      error("can't get here")
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
                  ExceptionUtil.catching(classOf[JsonReaderException]).either(parseJson[Message](tm.getText)) match {
                    case Right(Some(msg)) =>
                      service.messageReceived(msg)
                    case Right(None) =>
                      log.warn("Unable to parse JSON as a Message object: " + tm.getText)
                    case Left(exn) =>
                      log.warn("Received a non-JSON text message: " + tm.getText, exn)
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
