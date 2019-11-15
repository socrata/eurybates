package com.socrata.eurybates.message

// Be quiet!
// scalastyle:off

import scala.language.experimental.macros

import scala.annotation.StaticAnnotation

// Create a Message implementation that uses the given tag and an
// AutomaticJsonCodec to serialize the annotated class.
class AutomaticMessage(val tag: String) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro `-impl`.AutomaticMessageImpl.impl
}

package `-impl` {
  object AutomaticMessageImpl {
    def impl(c: Ctx)(annottees: c.Expr[Any]*): c.Expr[Any] = {
      import c.universe._

      val tag =
        c.prefix.tree match {
          case q"new AutomaticMessage(tag = $t)" => t
          case q"new AutomaticMessage($t)" => t
          case _ => c.abort(c.enclosingPosition, "Unexpected annotation syntax")
        }

      def msgCodec(name: TypeName) = {
        q"""
          implicit val ${freshTermName(c)} =
            _root_.com.socrata.eurybates.message.Message[$name]($tag)(_root_.com.rojoma.json.v3.util.AutomaticJsonEncodeBuilder[$name],
                                                                      _root_.com.rojoma.json.v3.util.AutomaticJsonDecodeBuilder[$name])
        """
      }

      def addCompanion(cls: ClassDef): c.Expr[Any] = {
        c.Expr(q"""
          $cls
          object ${cls.name.toTermName} {
            ${msgCodec(cls.name)}
          }
        """)
      }

      def modifyCompanion(cls: ClassDef, obj: ModuleDef): c.Expr[Any] = {
        val q"object $objname extends ..$objbases { ..$objbody }" = obj

        c.Expr(q"""
          $cls
          object $objname extends ..$objbases {
            ..$objbody
            ${msgCodec(cls.name)}
          }
        """)
      }

      annottees.map(_.tree) match {
        case List(cls: ClassDef) => addCompanion(cls)
        case List(cls: ClassDef, obj: ModuleDef) => modifyCompanion(cls, obj)
        case _ => c.abort(c.enclosingPosition, "Invalid annotation target")
      }
    }
  }
}
