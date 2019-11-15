package com.socrata.eurybates.message

import scala.reflect.macros._

package object `-impl` {
  type Ctx = blackbox.Context

  def freshTermName(c: Ctx) = c.universe.TermName(c.freshName())
}
