package com.socrata
package eurybates

import com.rojoma.json.v3.util.JsonUtil
import com.socrata.eurybates.Producer.ProducerType
import com.socrata.eurybates.Producer.ProducerType.ProducerType
import com.socrata.util.logging.LazyStringLogger

case class NoopProducer(sourceId: String) extends MessageCodec(sourceId) with Producer {
  val log = new LazyStringLogger(getClass)
  var message : Option[Message] = None

  def send(msg: Message): Unit = {
    message = Option(msg)
    message.foreach(someMessage =>
      log.info("Received request to send message: " + JsonUtil.renderJson(someMessage, true)))
  }

  def start: Unit = {}
  def stop: Unit = {}

  def supportedProducerTypes(): Seq[ProducerType] = {
    Seq(ProducerType.NoOp)
  }
}
