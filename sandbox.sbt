val startServer = taskKey[Unit]("start server.")
val stopServer = taskKey[Unit]("stop server.")
val numberTask = taskKey[Int]("number.")
val combinedInSeq = taskKey[Int]("number.")

startServer := {
  println("start")
}

stopServer := {
  println("stop")
}

numberTask := {
  1
}

val foo: Def.Initialize[Task[Int]] = {
  var n: Int = 0
  val t1 = Def.task { startServer.value; () }
  val t2 = Def.taskDyn { val _ = t1.value; Def.task { n = numberTask.value; () } }
  val t3 = Def.taskDyn { val _ = t2.value; Def.task { Thread.sleep(1000); () } }
  val t4 = Def.taskDyn { val _ = t3.value; Def.task { val x = stopServer.value; () } }
  Def.taskDyn { val _ = t4.value; Def.task { n } }
}

combinedInSeq := foo.value
