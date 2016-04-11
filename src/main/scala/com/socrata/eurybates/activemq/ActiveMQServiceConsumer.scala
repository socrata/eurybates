package com.socrata
package eurybates
package activemq

import java.util.concurrent.ExecutorService
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException

import javax.jms.{JMSException, Connection, Session, TextMessage}
import util.logging.LazyStringLogger
import com.rojoma.json.v3.io.JsonReaderException
import com.rojoma.json.v3.util.JsonUtil
import com.rojoma.json.v3.codec.DecodeError.InvalidValue
import com.rojoma.json.v3.ast.JString
import com.rojoma.json.v3.codec.Path

import com.socrata.eurybates.types.SessionMode


trait Transacted { this: ActiveMQServiceConsumer =>
  override def sessionMode = SessionMode.TRANSACTED
}

case class AMQRollbackMessageException() extends Exception

class ActiveMQServiceConsumer(connection: Connection, sourceId: String, executor: ExecutorService,
                              handlingLogger: (ServiceName, String, Throwable) => Unit,
                              services: Map[ServiceName, Service]) extends MessageCodec(sourceId) with QueueUtil {
  val log = new LazyStringLogger(getClass)


  private val workers = services map { case (serviceName, service) => new ServiceProcess(serviceName, service) }

  def sessionMode = SessionMode.NONE

  def start() : Unit = synchronized {
    workers.foreach(_.start())
  }

  def stop() : Unit = synchronized {
    workers.foreach(_.close())
    workers.foreach(_.join())
  }

  private class ServiceProcess(serviceName: ServiceName, service: Service) extends Thread {
    val transactional = sessionMode match {
      case SessionMode.TRANSACTED => true
      case _ => false
    }
    val amqSessionMode = sessionMode match {
      case SessionMode.TRANSACTED => Session.SESSION_TRANSACTED
      case _ => Session.AUTO_ACKNOWLEDGE
    }
    var session = connection.createSession(transactional, amqSessionMode)
    val queue = session.createQueue(queueName(serviceName))
    val consumer = session.createConsumer(queue)

    final val InitialSleepTime = 10L
    final val SleepTimeMaximum = 10000L

    setName(getId() + " / Eurybates activemq service " + serviceName)

    private def commit(): Unit = {
      sessionMode match {
        case SessionMode.TRANSACTED => {
          log.debug("Committing current AMQ transaction")
          session.commit()
        }
        case _ => /* Commit called but AMQ session is not transactional, ignoring. logging removed at user request */
      }
    }

    private def rollback(): Unit = {
      sessionMode match {
        case SessionMode.TRANSACTED => {
          log.debug("Rolling back current AMQ transaction")
          session.rollback()
        }
        case _ => /* Rollback called but AMQ session is not transactional, ignoring. logging removed at user request */
      }
    }

    private def nextMessage(): javax.jms.Message = {
      var sleepTime = InitialSleepTime
      val sleepMax = SleepTimeMaximum
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
      sys.error("Should never get here")
    }

    override def run(): Unit = {
      try {
        while(true) {
          val qMsg = nextMessage()
          if(qMsg == null) return

          // Why, you may ask, are we using a thread pool to handle an
          // event only to immediately block and wait for its result?
          // The answer: so that this thread may be interrupted with a
          // guarantee that this loop will terminate.

          val result = executor.submit(new Callable[Unit] {
            def call() = {
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
                      try {
                        service.messageReceived(m)
                        commit()
                      } catch {
                        case _: AMQRollbackMessageException => rollback()
                        case e: Throwable => {
                          rollback()
                          throw e
                        }
                      }
                    case Left(err) =>
                      log.warn("Received a non-JSON text message: " + err)
                      commit()
                  }
                case _ =>
                  log.warn("Received a non-TextMessage from JMS; actual type received is " + qMsg.getClass)
                  commit()
              }
            }
          })
          try {
            result.get()
          } catch {
            case e: InterruptedException => {
              // don't cancel it; we've already acknowledged the message
              // so let the executor finish handling it.
              throw e
            }
            case e: ExecutionException => {
              if(qMsg.isInstanceOf[TextMessage]) {
                handlingLogger(serviceName, qMsg.asInstanceOf[TextMessage].getText, e.getCause)
              } else {
                log.error("Received an unexpected exception while processing an instance of " + qMsg.getClass,
                  e.getCause)
              }
              rollback()
           }
          }
        }
      } catch {
        case e: InterruptedException => // time to go
      }
    }

    def close() : Unit = synchronized {
      consumer.close()
      session.close()
    }
  }
}
