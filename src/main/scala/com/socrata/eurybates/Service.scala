package com.socrata
package eurybates

import util.logging.LazyStringLogger
import SimpleService.log

trait Service {
  def messageReceived(message: Message): Unit
}

class SimpleService(consumers: Traversable[Consumer]) extends Service {

  private val routing: Map[Tag, List[Consumer]] =
    consumers.foldLeft(Map.empty[Tag, List[Consumer]].withDefault(_ => Nil)) { (acc, consumer) =>
      consumer.accepts.foldLeft(acc) { (acc, tag) =>
        acc + (tag -> (consumer :: acc(tag)))
      }
    }

  def messageReceived(message: Message): Unit = {
    log.info("Received " + message.tag)
    routing(message.tag).foreach(consumer => {
      log.info("Consuming using " + consumer.name)
      consumer.consume(message)
    })
    log.info("Finished processing " + message.tag)
  }
}

object SimpleService {
  val log = LazyStringLogger[SimpleService]
}
