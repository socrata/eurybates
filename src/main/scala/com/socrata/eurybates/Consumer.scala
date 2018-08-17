package com.socrata
package eurybates

import com.socrata.util.logging.LazyStringLogger

trait Consumer {
  val accepts: Set[(Tag, Option[SerialVersionUID])]
  def consume(message: Message): Unit
  def name: String = getClass.getName
}

trait VersionedConsumer extends Consumer {
  abstract override def consume(message: Message): Unit = {
    message._metadata match {
      case Some(metadata) =>
        if (!accepts.contains((message.tag, Some(metadata.serialVersionUID)))) {
          onVersionMismatch(message)
        } else {
          super.consume(message)
        }
      case None =>
        super.consume(message)
    }
  }

  def onVersionMismatch(message: Message): Unit
}

trait LoggingVersionedConsumer extends VersionedConsumer {
  val log = new LazyStringLogger(getClass)

  override def onVersionMismatch(message: Message): Unit = {
    log.warn(s"Version mismatch while consuming message ${message.details} (metadata: ${message._metadata}")
  }
}

trait ThrowingVersionedConsumer extends VersionedConsumer {
  final case class VersionMismatchException(private val message: String = "",
                                   private val cause: Throwable = None.orNull) extends Exception(message, cause)

  override def onVersionMismatch(message: Message): Unit = {
    throw VersionMismatchException(s"Version mismatch while consuming message ${message.details} (metadata: ${message._metadata}")
  }
}

trait Spying extends Consumer {
  abstract override def consume(message:Message): Unit = {
    spy(message)
    super.consume(message)
  }

  def spy(message:Message): Unit
}
