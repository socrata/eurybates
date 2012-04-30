package com.socrata
package eurybates

import util.logging.LazyStringLogger

trait Service {
  def messageReceived(message: Message)
}

class SimpleService(consumers: Traversable[Consumer]) extends Service {
  import SimpleService.log

  private val routing = consumers.foldLeft(Map.empty[Tag, List[Consumer]].withDefault(_ => Nil)) { (acc, consumer) =>
    consumer.accepts.foldLeft(acc) { (acc, tag) =>
      acc + (tag -> (consumer :: acc(tag)))
    }
  }

  def messageReceived(message: Message) {
    log.info("Received " + message.tag)
    for(consumer <- routing(message.tag)) {
      log.info("Consuming using " + consumer.name)
      consumer.consume(message)
    }
    log.info("Finished processing " + message.tag)
  }
}

object SimpleService {
  val log = LazyStringLogger[SimpleService]
}
