package com.socrata
package eurybates

class NullProducer extends Producer {
  var message: Message = null

  def apply(msg: Message) {
    message = msg
  }
}
