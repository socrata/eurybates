package com.socrata
package eurybates

import com.socrata.eurybates.Producer.ProducerType
import com.socrata.eurybates.Producer.ProducerType
import com.socrata.eurybates.Producer.ProducerType.ProducerType

import java.util.concurrent.ExecutorService
import java.util.concurrent.Callable
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ExecutionException

class LocalServiceWrangler(executor: ExecutorService, handlingLogger: (ServiceName, Message, Throwable) => Unit, services: Map[ServiceName, Service]) extends Producer {
  private val workers = services map { case (serviceName, service) => new ServiceProcess(serviceName, service) }

  def send(msg: Message) {
    val forcedDetails = msg.details.forced
    val forcedMsg = msg.copy(details = forcedDetails)

    // TODO: if activemq composite destinations does not put messages
    // on the composite queue atomically, this does not need to be
    // synchronized.  But I don't know whether or not it does.
    synchronized {
      for(worker <- workers) worker.queue.add(forcedMsg)
    }
  }

  def supportedProducerTypes() : Seq[ProducerType] = {
    Seq(ProducerType.LocalService)
  }

  def start() = synchronized {
    workers.foreach(_.start())
  }

  def stop() = synchronized {
    workers.foreach(_.interrupt())
    workers.foreach(_.join())
  }

  private class ServiceProcess(serviceName: ServiceName, service: Service) extends Thread {
    val queue = new LinkedBlockingQueue[Message]

    setName(getId() + " / Eurybates service " + serviceName)

    override def run() {
      try {
        while(!isInterrupted) {
          val msg = queue.take()

          // Why, you may ask, are we using a thread pool to handle an
          // event only to immediately block and wait for its result?
          // The answer: so that this thread may be interrupted with a
          // guarantee that this loop will terminate.

          val result = executor.submit(new Callable[Unit] {
            def call() { service.messageReceived(msg) }
          })
          try {
            result.get()
          } catch {
            case e: InterruptedException =>
              throw e
            case e: ExecutionException =>
              handlingLogger(serviceName, msg, e.getCause)
          }
        }
      } catch {
        case _: InterruptedException =>
          // time to go
      }
    }
  }
}
