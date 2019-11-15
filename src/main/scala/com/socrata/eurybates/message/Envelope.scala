package com.socrata.eurybates.message

import com.rojoma.json.v3.ast.JValue

final class Envelope private (val tag: String, val details: JValue) {
  def decode[T](implicit ev: Message[T]): Option[T] =
    if(ev.tag == tag) ev.decode(details).right.toOption
    else None

  def forced = new Envelope(tag, details.forced)

  override def toString = s"Envelope($tag, $details)"
}

object Envelope {
  def apply[T](msg: T)(implicit ev: Message[T]): Envelope =
    new Envelope(ev.tag, ev.encode(msg))

  private[eurybates] def fromParts(tag: String, value: JValue) =
    new Envelope(tag, value)
}
