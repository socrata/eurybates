package com.socrata
package eurybates

import com.rojoma.json.v3.util.JsonUtil
import com.socrata.eurybates.Producer.ProducerType
import com.socrata.eurybates.Producer.ProducerType.ProducerType
import com.socrata.util.logging.LazyStringLogger

case class NoopProducer(sourceId: String) extends MessageCodec(sourceId) with Producer {
  val log = new LazyStringLogger(getClass)
  var message : Message = null

  def send(msg: Message) {
    message = msg
    log.info("Received request to send message: " + JsonUtil.renderJson(msg, true))
  }

  def start = {}
  def stop = {}

  def supportedProducerTypes(): Seq[ProducerType] = {
    Seq(ProducerType.NoOp)
  }
}
