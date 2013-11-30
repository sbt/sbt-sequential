name := "helloworld"

organization := "com.example"

version := "0.1.0-SNAPSHOT"

val someTask1 = taskKey[String]("some task")
val someTask2 = taskKey[String]("some task")
val someTask3 = taskKey[String]("some task")

// normal task
someTask1 := {
  println("starting...")
  Thread.sleep(500)
  "a"
}

// normal task
someTask2 := {
  println("ending...")
  Thread.sleep(500)
  "c"
}

// sequential task
val task1 = Def.sequentialTask {
  val x = someTask1.value
  val y = "b"
  println("now task1...")
  Thread.sleep(500)
  val z = someTask2.value
  x + y + z
}

someTask3 := task1.value

val check = taskKey[Unit]("checks this plugin")

check := {
  val x = someTask3.value
  x match {
    case "abc" =>
    case _ => sys.error("unexpected output: " + x)
  }
  ()
}
