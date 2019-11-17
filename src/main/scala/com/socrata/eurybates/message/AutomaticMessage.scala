package com.socrata.eurybates.message

// Be quiet!
// scalastyle:off

import scala.language.experimental.macros

import scala.annotation.StaticAnnotation

import java.security.MessageDigest
import java.nio.charset.StandardCharsets

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

      def sha1sum(s: String) = {
        val md = MessageDigest.getInstance("SHA1")
        md.digest(s.getBytes(StandardCharsets.UTF_8)).map(_.toInt & 0xff).map("%02x".format(_)).mkString
      }

      def msgCodec(name: TypeName) = {
        // could use freshName for this, but this has guaranteed
        // stability and while it's not _guaranteed_ that it won't
        // collide with anything, you'd have to work at it.
        val fieldName = toTermName(c, "automatically generated message codec " + sha1sum(name.encodedName.toString))
        q"""
          implicit val $fieldName =
            _root_.com.socrata.eurybates.message.Message[$name]($tag)(_root_.com.rojoma.json.v3.util.AutomaticJsonEncodeBuilder[$name],
                                                                      _root_.com.rojoma.json.v3.util.AutomaticJsonDecodeBuilder[$name])
        """
      }

      def addCompanion(cls: ClassDef): c.Expr[Any] = {
        val objModFlags = Seq(Flag.PRIVATE, Flag.PROTECTED, Flag.LOCAL).foldLeft(NoFlags) { (set, flag) =>
          if(cls.mods.hasFlag(flag)) set | flag
          else set
        }
        val objMod = Modifiers(objModFlags, cls.mods.privateWithin, Nil)

        if(cls.mods.hasFlag(Flag.CASE)) {
          // case class companion objects are special.  They extend
          // Serializable, are Functions, and have a toString that
          // overrides the one inherited from Function.
          val ctorInfo = cls.impl.children.collectFirst {
            case DefDef(_, name, _, pparams, _, _) if name.decodedName.toString == "<init>" =>
              pparams
          }

          val parents = ctorInfo match {
            case Some(List(paramList)) if paramList.length < 23 =>
              val afn = toTypeName(c, "AbstractFunction" + paramList.length)
              val params = paramList.map { param =>
                val Scala = toTermName(c, "Scala") // 2.10 grr
                val Repeated = toTypeName(c, "<repeated>") // 2.10 grr
                  param.tpt match {
                    case AppliedTypeTree(Select(Select(Ident(termNames.ROOTPKG), Scala), Repeated), List(t)) =>
                      tq"_root_.scala.Seq[$t]"
                    case other =>
                      other
                  }
              }
              List(tq"_root_.scala.runtime.$afn[..$params, ${cls.name}]", tq"_root_.scala.Serializable")
            case _ => List(tq"_root_.scala.Serializable")
          }

          c.Expr(q"""
            $cls
            $objMod object ${cls.name.toTermName} extends ..$parents {
              override def toString = ${cls.name.decodedName.toString}
              ${msgCodec(cls.name)}
            }
          """)
        } else {
          c.Expr(q"""
            $cls
            $objMod object ${cls.name.toTermName} {
              ${msgCodec(cls.name)}
            }
          """)
        }
      }

      def modifyCompanion(cls: ClassDef, obj: ModuleDef): c.Expr[Any] = {
        val newObj =
          ModuleDef(obj.mods, obj.name, Template(obj.impl.parents, obj.impl.self, obj.impl.body :+ msgCodec(cls.name)))

        c.Expr(q"""
          $cls
          $newObj
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
