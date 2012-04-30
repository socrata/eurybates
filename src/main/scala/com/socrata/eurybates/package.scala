package com.socrata

package object eurybates {
  type ServiceName = String
  type Tag = String

  /** A Producer accepts messages from user code and routes them to
    * all services. */
  type Producer = Message => Unit
}
