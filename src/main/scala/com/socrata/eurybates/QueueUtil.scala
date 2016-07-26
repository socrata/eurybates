package com.socrata.eurybates

trait QueueUtil {
  def queueName(service: ServiceName): String = Name + "." + service
}
