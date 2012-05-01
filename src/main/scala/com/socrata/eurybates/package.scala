package com.socrata

package object eurybates {
  type ServiceName = String
  type Tag = String

  /** A Producer accepts messages from user code and routes them to a topic.
   *
   * In the ActiveMQ implementation, this is sent to all services based on a registry maintained
   * in ZooKeeper, and the consumer code is responsible for filtering out which messages it wants.
   *
   * In the Kafka implementation, each Message is sent to a specific topic. The consumer code only
   * receives messages on the topic(s) they wish to subscribe to.
   */
  type Producer = Message => Unit
}
