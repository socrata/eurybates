package com.socrata.eurybates.message

import scala.reflect.macros._

package object `-impl` {
  type Ctx = Context
  def freshTermName(c: Ctx) = c.universe.newTermName(c.fresh())
}
