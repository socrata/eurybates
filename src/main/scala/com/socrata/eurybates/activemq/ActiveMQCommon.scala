package com.socrata
package eurybates
package activemq

import com.rojoma.json.ast.JValue
import com.rojoma.json.codec.JsonCodec
import com.rojoma.json.matcher._

class ActiveMQCommon(sourceId: String) {
  private val tagVar = Variable[Tag]
  private val detailsVar = Variable[JValue]
  private val sourceIdVar = Variable[String]
  private val uuidVar = Variable[String]
  private val WireMessagePat = PObject(
    "tag" -> tagVar,
    "details" -> detailsVar,
    "source_id" -> sourceIdVar,
    "uuid" -> uuidVar
  )

  implicit object MessageCodec extends JsonCodec[Message] {
    def encode(msg: Message): JValue =
      WireMessagePat.generate(
        tagVar := msg.tag,
        detailsVar := msg.details,
        sourceIdVar := sourceId,
        uuidVar := java.util.UUID.randomUUID().toString)

    def decode(x: JValue): Option[Message] = WireMessagePat.matches(x) map { results =>
      Message(tagVar(results), detailsVar(results))
    }
  }

  def queueName(service: ServiceName) = "eurybates." + service
}

