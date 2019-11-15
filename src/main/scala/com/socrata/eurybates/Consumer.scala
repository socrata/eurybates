package com.socrata
package eurybates

import com.socrata.eurybates.message.{Envelope, Message}

trait Consumer {
  val accepts: Set[Tag]
  def consume(envelope: Envelope): Unit
  def name: String = getClass.getName
}

object Consumer {
  private trait ConsumerPair {
    type MessageType
    val message: Message[MessageType]
    val consume: MessageType => Unit
  }

  class Builder private (private val consumers: Map[String, ConsumerPair], name: String) {
    def this(name: String) = this(Map.empty, name)
    def this() = this("Consumer.Builder.Consumer")

    def consuming[T : Message](onMessage: T => Unit) = {
      val consumer = new ConsumerPair {
        type MessageType = T
        val message = implicitly[Message[MessageType]]
        val consume = onMessage
      }
      new Builder(consumers + (consumer.message.tag -> consumer), name)
    }

    def build(): Consumer =
      new Consumer {
        val accepts = consumers.keySet
        override val name = Builder.this.name
        def consume(envelope: Envelope): Unit = {
          consumers.get(envelope.tag) match {
            case Some(consumer) =>
              envelope.decode(consumer.message) match {
                case Some(msg) =>
                  consumer.consume(msg)
                case None =>
                  // ignore
              }
            case None =>
              // ignore
          }
        }
      }
  }

  object Builder extends Builder {
    def apply(name: String) = new Builder(name)

    // use this like either:
    //
    //   Consumer.Builder.
    //     consume[SomeMessage] { msg => ... }.
    //     consume[SomeOtherMessage] { msg => ... }.
    //     build()
    //
    // or give it a name:
    //
    //   Consumer.Builder("my composite consumer").
    //     consume[SomeMessage] { msg => ... }.
    //     consume[SomeOtherMessage] { msg => ... }.
    //     build()
  }
}

abstract class Spying(underlying: Consumer) extends Consumer {
  override val accepts = underlying.accepts
  override def consume(envelope: Envelope): Unit = {
    spy(envelope)
    underlying.consume(envelope)
  }

  def spy(message: Envelope): Unit
}
