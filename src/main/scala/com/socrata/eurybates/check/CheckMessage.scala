package com.socrata.eurybates.check

import com.rojoma.json.v3.ast.{JNull, JValue}
import com.rojoma.json.v3.codec.JsonDecode

import com.socrata.eurybates.message.Message

class CheckMessage(tag: String) {
  object Message {
    implicit val msg = new Message[Message.type] {
      val tag = CheckMessage.this.tag
      def encode(m: Message.type) = JNull
      def decode(v: JValue) = JsonDecode.fromJValue[JNull](v).right.map { _ => Message }
    }
  }
}

object CheckMessage {
  object Hello extends CheckMessage("hello")
}
