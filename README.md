sbt-sequential
==============

sbt-sequential adds sequential tasks to sbt.

Lastest
-------

This is an experimental plugin. Add this to `project/sequential.sbt`:

```scala
addSbtPlugin("com.eed3si9n" % "sbt-sequential" % "0.1.0")
```

Usage
-----

This plugin implicitly adds `Def.sequentialTask` method, which defines a sequential task.

```scala
val startServer = taskKey[Unit]("start server.")
val stopServer = taskKey[Unit]("stop server.")
val integrationTest = taskKey[Int]("integration test.")

// normal task
startServer := {
  println("start")
}

// normal task
stopServer := {
  println("stop")
}

// sequential task
val task1 = Def.sequentialTask {
  startServer.value
  val n = 1
  Thread.sleep(2000)
  stopServer.value
  n
}

integrationTest := task1.value
```

License
-------

MIT License. Copyright [@eed3si9n](https://twitter.com/eed3si9n) (Eugene Yokota).
