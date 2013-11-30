package sbtsequential

import sbt._
import Keys._
import scala.collection.immutable.Seq
import collection.mutable
import scala.reflect.macros.Context

object Plugin extends sbt.Plugin {
  implicit def defToDefOps(x: sbt.Def.type): DefOps.type = DefOps
}

case object DefOps {
  import language.experimental.macros
  import CustomTaskMacro.{sequentialTaskMacroImpl, debugSequentialTaskMacroImpl}
  def sequentialTask[T](t: T): Def.Initialize[Task[T]] = macro sequentialTaskMacroImpl[T]
  def debugSequentialTask[T](t: T): Def.Initialize[Task[T]] = macro debugSequentialTaskMacroImpl[T] 
}

object CustomTaskMacro {
  import language.experimental.macros
  import scala.reflect._
  import reflect.macros._
  import reflect.internal.annotations.compileTimeOnly

  def debugSequentialTaskMacroImpl[T0: c0.WeakTypeTag](c0: Context)(t: c0.Expr[T0]): c0.Expr[Def.Initialize[Task[T0]]] =
    throw new Exception(sequentialTaskMacroImpl(c0)(t).tree.toString)

  def sequentialTaskMacroImpl[T0: c0.WeakTypeTag](c0: Context)(t: c0.Expr[T0]): c0.Expr[Def.Initialize[Task[T0]]] = {
    import c0.universe.{ Apply => ApplyTree, _ }
    val util = new { type T = T0; val c: c0.type = c0 } with CustomMacroUtil
    import util.{ c => _, _ }

    def emulateFlatMaps(trees0: Vector[Tree], tree0: Tree): Expr[Def.Initialize[Task[T0]]] = {
      val taskDyns = trees0.zipWithIndex map { case (t, idx) =>
        if (idx == 0) declareValWrapInTask(taskName(0), t)
        else declareValWrapInTaskDyn(taskName(idx), t, taskName(idx - 1))
      }
      buff ++= taskDyns
      c0.Expr[Def.Initialize[Task[T0]]](Block(
        buff.toList,
        reify(Def.taskDyn {
          c0.Expr[Unit](declareDummyVal(taskName(trees0.size - 1))).splice
          Def.task { c0.Expr[T0](moveValOut(tree0)).splice }
        }).tree
      ))
    }
    val expr = t.tree match {
      case Block(Nil, x) => wrapInTask(x)
      case Block(xs, x)  => emulateFlatMaps(xs.toVector, x)
      case x             => wrapInTask(x)
    }
    expr
  }
}

trait CustomMacroUtil {
  type T
  val c: Context
  import c.universe.{ Apply => ApplyTree, _ }
  private val nameCache: mutable.Map[String, String] = mutable.Map()
  val buff = mutable.ListBuffer[Tree]()

  def VAL(name: String)(rhs: => Tree): Tree = ValDef(NoMods, name, TypeTree(), rhs)
  var freshNumber: Int = 0
  def fresh(): String = {
    freshNumber = freshNumber + 1
    "_sbtsequential_" + freshNumber.toString
  } 
  def freshNames(key: String): String = nameCache getOrElseUpdate (key, fresh()) 
  def taskName(idx: Int): String = freshNames("$task" + idx.toString)
  def valName(orig: String): String = freshNames("$val" + orig)
  def hasValName(orig: String): Boolean = nameCache contains ("$val" + orig)

  def wrapInTask(tree: Tree): Expr[Def.Initialize[Task[T]]] =
    reify(Def.task { c.Expr[T](tree).splice })
  def wrapInTaskWithUnit(tree: Tree): Expr[Def.Initialize[Task[Unit]]] =
    reify(Def.task {
      c.Expr[Unit](tree).splice
      ()
    })
  def declareDummyVal(prev: String): Tree =
    VAL("_") (Select(Ident(newTermName(prev)), newTermName("value")))
  def declareValWrapInTask(name: String, body: Tree): Tree =
    VAL(name) (wrapInTaskWithUnit(moveValOut(body)).tree)
  def declareValWrapInTaskDyn(name: String, body: Tree, prev: String): Tree =
      VAL(name) (reify(Def.taskDyn {
        c.Expr[Def.Initialize[Task[Unit]]](Block(
          List(declareDummyVal(prev)),
          wrapInTaskWithUnit(moveValOut(body)).tree
        )).splice
      }).tree)
  
  private val trans = new Transformer {
    override def transform(tree: Tree): Tree = tree match {
      case ValDef(mods, name0, tt, rhs) =>
        import definitions._
        val tpe0: Type = if (!tt.isEmpty) tt.tpe
                         else rhs.tpe.normalize
        val (tpe, init) = tpe0 match {
          case x if x <:< ByteTpe    => (ByteTpe,    Literal(Constant(0))) 
          case x if x <:< ShortTpe   => (ShortTpe,   Literal(Constant(0)))
          case x if x <:< IntTpe     => (IntTpe,     Literal(Constant(0)))
          case x if x <:< LongTpe    => (LongTpe,    Literal(Constant(0)))
          case x if x <:< FloatTpe   => (FloatTpe,   Literal(Constant(0)))
          case x if x <:< DoubleTpe  => (DoubleTpe,  Literal(Constant(0)))
          case x if x <:< BooleanTpe => (BooleanTpe, Literal(Constant(false)))
          case x if x <:< CharTpe    => (CharTpe,    Literal(Constant(' ')))
          case x if x <:< UnitTpe    => (UnitTpe,    Literal(Constant(())))
          case _ => (tpe0, Literal(Constant(null)))
        }
        val name = newTermName(valName(name0.toString))
        val varTree = ValDef(Modifiers(Flag.MUTABLE), name, TypeTree(tpe), init)
        buff += varTree
        Assign(Ident(name), rhs)
      case Ident(name0) =>
        val name = if (hasValName(name0.toString)) newTermName(valName(name0.toString))
                   else name0
        Ident(name) // name fresh identifier tree
      case _ => super.transform(tree)   
    }

  }
  def moveValOut(tree: Tree): Tree = trans.transform(tree)
}
