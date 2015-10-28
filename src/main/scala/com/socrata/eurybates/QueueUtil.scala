package com.socrata.eurybates

trait QueueUtil {
  def queueName(service: ServiceName) = Name + "." + service
}
