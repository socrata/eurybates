package com.socrata.eurybates

object SessionMode {
  sealed trait SessionMode
  case object Transacted extends SessionMode
  case object None extends SessionMode
}
