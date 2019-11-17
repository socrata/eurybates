package com.socrata.eurybates

import com.rojoma.json.v3.ast.JValue
import com.rojoma.json.v3.matcher.{PObject, Variable}
import com.rojoma.json.v3.codec.{DecodeError, JsonDecode, JsonEncode}

import com.socrata.eurybates.message.Envelope

class EnvelopeCodec(sourceId:String) {
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

  implicit object jCodec extends JsonEncode[Envelope] with JsonDecode[Envelope] {
    def encode(msg: Envelope): JValue =
      WireMessagePat.generate(
        tagVar := msg.tag,
        detailsVar := msg.details,
        sourceIdVar := sourceId,
        uuidVar := java.util.UUID.randomUUID().toString)

    def decode(x: JValue): Either[DecodeError, Envelope] =
      WireMessagePat.matches(x).right.map { results =>
        Envelope.fromParts(tagVar(results), detailsVar(results))
      }

  }
}
