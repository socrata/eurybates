package com.socrata.eurybates.message

import com.rojoma.json.v3.ast.JValue
import com.rojoma.json.v3.codec.{JsonEncode, JsonDecode, DecodeError}

trait Message[T] {
  val tag: String
  def encode(msg: T): JValue
  def decode(value: JValue): Either[DecodeError, T]
}

class JCodecMessage[T : JsonEncode : JsonDecode](val tag: String) extends Message[T] {
  def encode(msg: T) = JsonEncode.toJValue(msg)
  def decode(value: JValue) = JsonDecode.fromJValue(value)
}

object Message {
  def apply[T : JsonEncode : JsonDecode](tag: String): Message[T] =
    new JCodecMessage[T](tag)
}
