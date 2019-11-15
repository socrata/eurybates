package com.socrata
package eurybates

import util.logging.LazyStringLogger
import SimpleService.log

import com.socrata.eurybates.message.Envelope

trait Service {
  def messageReceived(message: Envelope): Unit
}

class SimpleService(consumers: Traversable[Consumer]) extends Service {

  private val routing: Map[Tag, List[Consumer]] =
    consumers.foldLeft(Map.empty[Tag, List[Consumer]].withDefault(_ => Nil)) { (acc, consumer) =>
      consumer.accepts.foldLeft(acc) { (acc, tag) =>
        acc + (tag -> (consumer :: acc(tag)))
      }
    }

  def messageReceived(message: Envelope): Unit = {
    log.debug("Received " + message.tag)
    routing(message.tag).foreach(consumer => {
      log.info(s"Consuming ${message.tag} using ${consumer.name}")
      consumer.consume(message)
    })
    log.debug("Finished processing " + message.tag)
  }
}

object SimpleService {
  val log = LazyStringLogger[SimpleService]
}
