package sbtsequential

import sbt._
import Keys._
import scala.collection.immutable.Seq
import collection.mutable

object Plugin extends sbt.Plugin {
  implicit def defToDefOps(x: sbt.Def.type): DefOps.type = DefOps
}

case object DefOps {
  import language.experimental.macros
  import CustomTaskMacro.sequentialTaskMacroImpl
  def sequentialTask[T](t: T): Def.Initialize[Task[T]] = macro sequentialTaskMacroImpl[T]
}

object CustomTaskMacro {
  import language.experimental.macros
  import scala.reflect._
  import reflect.macros._
  import reflect.internal.annotations.compileTimeOnly

  def sequentialTaskMacroImpl[T: c.WeakTypeTag](c: Context)(t: c.Expr[T]): c.Expr[Def.Initialize[Task[T]]] = {
    import c.universe.{ Apply => ApplyTree, _ }
    val buff = mutable.ListBuffer[Tree]()
    val nameCache: mutable.Map[String, String] = mutable.Map()
    def freshNames(key: String): String = nameCache getOrElseUpdate (key, c.fresh()) 
    def taskName(idx: Int): String = freshNames("task" + idx.toString)
    def wrapInTask(tree: Tree): Expr[Def.Initialize[Task[T]]] =
      reify(Def.task { c.Expr[T](tree).splice })
    def declareDummyVal(prev: String): Tree =
      ValDef(NoMods, "_", TypeTree(), Select(Ident(newTermName(prev)), newTermName("value")))
    def declareValWrapInTask(name: String, body: Tree): Tree =
      ValDef(NoMods, name, TypeTree(), wrapInTask(body).tree)      
    def declareValWrapInTaskDyn(name: String, body: Tree, prev: String): Tree =
      ValDef(NoMods, name, TypeTree(), reify(Def.taskDyn {
        c.Expr[Def.Initialize[Task[Unit]]](Block(
          List(declareDummyVal(prev)),
          wrapInTask(body).tree
        )).splice
      }).tree)
    def emulateFlatMaps(trees0: Vector[Tree], tree0: Tree): Expr[Def.Initialize[Task[T]]] = {
      val transformed = trees0.zipWithIndex map { case (t, idx) =>
        if (idx == 0) declareValWrapInTask(taskName(0), t)
        else declareValWrapInTaskDyn(taskName(idx), t, taskName(idx - 1))
      }
      buff ++= transformed
      c.Expr[Def.Initialize[Task[T]]](Block(
        buff.toList,
        reify(Def.taskDyn {
          c.Expr[Unit](declareDummyVal(taskName(trees0.size - 1))).splice
          Def.task { c.Expr[T](tree0).splice }
        }).tree
      ))
    }
    val expr = t.tree match {
      case Block(Nil, x) => wrapInTask(x)
      // case Block(xs, x)  => throw new Exception(emulateFlatMaps(xs.toVector, x).tree.toString)
      case Block(xs, x)  => emulateFlatMaps(xs.toVector, x)
      case x             => wrapInTask(x)
    }
    expr
  }
}
