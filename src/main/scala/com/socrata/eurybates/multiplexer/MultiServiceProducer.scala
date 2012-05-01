package com.socrata.eurybates.multiplexer

import com.socrata.eurybates.{Message, MessageCodec, Producer,Consumer}

/** A producer that sends to multiple sub-producers
 *
 * Startup and shutdown/cleanup operations are not considered part of the lifecycle of this. You should ensure that
 * your individual Producer objects are already initialized, and clean them up when you're done.
 */
class MultiServiceProducer(sourceId:String, producers:List[Producer]) extends MessageCodec(sourceId) with Producer {
  def apply(message: Message) {
    for(producer <- producers) {
      producer.apply(message)
    }
  }
}
