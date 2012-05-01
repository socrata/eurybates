package com.socrata
package eurybates

trait Consumer {
  val accepts: Set[Tag]
  def consume(message: Message)
  def name: String = getClass.getName
}


trait Spying extends Consumer {
  abstract override def consume(message:Message) = {
    spy(message)
    super.consume(message)
  }

  def spy(message:Message)


}
