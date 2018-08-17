package com.socrata
package eurybates

import com.rojoma.json.v3.ast.JValue

case class Metadata(serialVersionUID: SerialVersionUID)

case class Message(tag: Tag, details: JValue, _metadata: Option[Metadata])

object Message {
  def apply(tag: Tag, details: JValue): Message = Message(tag, details, None)
}
