package com.socrata.eurybates.message

import scala.reflect.macros._

package object `-impl` {
  type Ctx = Context
  def freshTermName(c: Ctx) = c.universe.newTermName(c.fresh())
  def toTermName(c: Ctx, s: String) = c.universe.newTermName(s)
  def toTypeName(c: Ctx, s: String) = c.universe.newTypeName(s)

  object termNames {
    val ROOTPKG = "_root_"
  }
}
