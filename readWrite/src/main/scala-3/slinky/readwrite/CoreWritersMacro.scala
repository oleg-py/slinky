package slinky.readwrite

import scala.deriving._
import scala.compiletime._
import scalajs.js

trait MacroWriters {
//  implicit def deriveWriter[T]: Writer[T] = macro MacroWritersImpl.derive[T]

  inline implicit def deriveProduct[T] (using m: Mirror.ProductOf[T]): Writer[T] = {
    val labels = constValueTuple[m.MirroredElemLabels]
    val writers = summonAll[Tuple.Map[m.MirroredElemTypes, Writer]]
    new Writer[T] {
      def write(p: T): js.Object = {
        val d = js.Dictionary[js.Object]()
        labels.productIterator
          .zip(writers.productIterator)
          .zip(p.asInstanceOf[Product].productIterator)
          .foreach { case ((label, writer), value) =>
            d(label.asInstanceOf[String]) = writer.asInstanceOf[Writer[_]].write(value.asInstanceOf)
          }
        d.asInstanceOf[js.Object]
      }
    }
  }

  inline implicit def deriveSum[T](using m: Mirror.SumOf[T]): Writer[T] = {
    val labels = constValueTuple[m.MirroredElemLabels]
    val writers = summonAll[Tuple.Map[m.MirroredElemTypes, Writer]]
    new Writer[T] {
      def write(p: T): js.Object = {
        val ord = m.ordinal(p)
        val typ = labels.productElement(ord)
        val base = writers.productElement(ord).asInstanceOf[Writer[T]].write(p)
        base.asInstanceOf[js.Dynamic]._type = typ.asInstanceOf[js.Any]
        base.asInstanceOf[js.Dynamic]._ord = ord
        base
      }
    }
  }
}

//class MacroWritersImpl(_c: whitebox.Context) extends GenericDeriveImpl(_c) {
//  import c.universe._
//
//  val typeclassType: c.universe.Type = typeOf[Writer[_]]
//
//  def deferredInstance(forType: Type, constantType: Type) =
//    q"new _root_.slinky.readwrite.DeferredWriter[$forType, $constantType]"
//
//  def maybeExtractDeferred(tree: Tree): Option[Tree] =
//    tree match {
//      case q"new _root_.slinky.readwrite.DeferredWriter[$_, $t]()" =>
//        Some(t)
//      case q"new slinky.readwrite.DeferredWriter[$_, $t]()" =>
//        Some(t)
//      case _ => None
//    }
//
//  def createModuleTypeclass(tpe: Type, moduleReference: Tree): Tree =
//    q"""new _root_.slinky.readwrite.Writer[$tpe] {
//          def write(v: $tpe): _root_.scala.scalajs.js.Object = {
//            _root_.scala.scalajs.js.Dynamic.literal()
//          }
//        }"""
//
//  def createCaseClassTypeclass(clazz: Type, params: Seq[Seq[Param]]): Tree = {
//    val paramsTrees = params.flatMap(_.map { p =>
//      q"""{
//         val writtenParam = ${getTypeclass(p.tpe)}.write(v.${p.name.toTermName})
//         if (!_root_.scala.scalajs.js.isUndefined(writtenParam)) {
//           ret.${TermName(p.name.encodedName.toString)} = writtenParam
//         }
//       }"""
//    })
//
//    q"""new _root_.slinky.readwrite.Writer[$clazz] {
//          def write(v: $clazz): _root_.scala.scalajs.js.Object = {
//            val ret = _root_.scala.scalajs.js.Dynamic.literal()
//            ..$paramsTrees
//            ret
//          }
//        }"""
//  }
//
//  def createValueClassTypeclass(clazz: Type, param: Param): Tree =
//    q"""new _root_.slinky.readwrite.Writer[$clazz] {
//          def write(v: $clazz): _root_.scala.scalajs.js.Object = {
//            ${getTypeclass(param.tpe)}.write(v.${param.name.toTermName})
//          }
//        }"""
//
//  def createSealedTraitTypeclass(traitType: Type, subclasses: Seq[Symbol]): Tree = {
//    val cases = subclasses.map { sub =>
//      cq"""(value: $sub) =>
//             val ret = ${getTypeclass(sub.asType.toType)}.write(value)
//             ret.asInstanceOf[_root_.scala.scalajs.js.Dynamic]._type = ${sub.name.toString}
//             ret"""
//    }
//
//    q"""new _root_.slinky.readwrite.Writer[$traitType] {
//          def write(v: $traitType): _root_.scala.scalajs.js.Object = {
//            v match {
//              case ..$cases
//              case _ => _root_.slinky.readwrite.Writer.fallback[$traitType].write(v)
//            }
//          }
//        }"""
//  }
//
//  def createFallback(forType: Type) = q"_root_.slinky.readwrite.Writer.fallback[$forType]"
//}
