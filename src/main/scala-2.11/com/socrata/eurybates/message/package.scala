package com.socrata.eurybates.message

import scala.reflect.macros._

package object `-impl` {
  type Ctx = blackbox.Context

  def freshTermName(c: Ctx) = c.universe.TermName(c.freshName())
  def toTermName(c: Ctx, s: String) = c.universe.TermName(s)
  def toTypeName(c: Ctx, s: String) = c.universe.TypeName(s)
}
