package com.socrata
package eurybates

import com.rojoma.json.v3.ast.JValue
import com.rojoma.json.v3.util.AutomaticJsonCodecBuilder

case class Message(tag: Tag, details: JValue)
/*object Message {
  implicit val jCodec = AutomaticJsonCodecBuilder[Message]
} */
