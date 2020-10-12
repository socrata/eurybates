package com.socrata.eurybates

import com.rojoma.json.v3.ast.JValue
import com.rojoma.json.v3.matcher.{PObject, Variable}
import com.rojoma.json.v3.codec.{DecodeError, JsonDecode, JsonEncode}

class MessageCodec(sourceId:String) {
  private val tagVar = Variable[Tag]
  private val detailsVar = Variable[JValue]
  private val sourceIdVar = Variable[String]
  private val uuidVar = Variable[String]
  private val activitiesVar = Variable[JArray]
  private val WireMessagePat = PObject(
    "tag" -> tagVar,
    "details" -> detailsVar,
    "source_id" -> sourceIdVar,
    "uuid" -> uuidVar,
    "activities" -> activitiesVar
  )

  implicit object jCodec extends JsonEncode[Message] with JsonDecode[Message] {
    def encode(msg: Message): JValue =
      WireMessagePat.generate(
        tagVar := msg.tag,
        detailsVar := msg.details,
        sourceIdVar := sourceId,
        uuidVar := java.util.UUID.randomUUID().toString,
        activities := msg.activities)

    def decode(x: JValue): Either[DecodeError, Message] =
      WireMessagePat.matches(x).right.map { results =>
        Message(tagVar(results), detailsVar(results), activitiesVar(results))
      }

  }
}
