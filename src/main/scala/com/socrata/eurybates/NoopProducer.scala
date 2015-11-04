package com.socrata
package eurybates

class NoopProducer extends Producer {
  var message: Message = null

  def apply(msg: Message) {
    message = msg
  }

  def start = {}
  def stop = {}
}
