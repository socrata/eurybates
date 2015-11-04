package com.socrata
package eurybates

import com.rojoma.json.v3.util.JsonUtil
import com.socrata.util.logging.LazyStringLogger

case class NoopProducer(sourceId: String) extends MessageCodec(sourceId) with Producer {
  val log = new LazyStringLogger(getClass)

  def send(msg: Message) {
    log.info("Received request to send message: " + JsonUtil.renderJson(msg, true))
  }

  def start = {}
  def stop = {}
}
