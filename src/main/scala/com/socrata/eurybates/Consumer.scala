package com.socrata
package eurybates

trait Consumer {
  val accepts: Set[Tag]
  def consume(message: Message)
  def name: String = getClass.getName
}
