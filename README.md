sbt-sequential
==============

sbt-sequential adds sequential tasks to sbt.

**NOTE:**

[0.13.8-notes]: http://www.scala-sbt.org/0.13/docs/sbt-0.13-Tech-Previews.html#Sequential+tasks
[Sequencing]: http://www.scala-sbt.org/0.13/docs/Howto-Sequencing.html

This plugin has been folded into sbt proper, as of v0.13.8 (see the [release notes][0.13.8-notes]).

Also the topic of sequencing in sbt is now covered in the [Sequencing][] section in the docs.

Lastest
-------

This is an experimental plugin. Add this to `project/sequential.sbt`:

```scala
addSbtPlugin("com.eed3si9n" % "sbt-sequential" % "0.1.0")
```

Usage
-----

This plugin implicitly adds `Def.sequentialTask[T]` method, which returns a sequential task.

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
lazy val integrationTestImpl = Def.sequentialTask {
  startServer.value
  val n = 1
  Thread.sleep(2000)
  stopServer.value
  n
}

integrationTest := integrationTestImpl.value
```

See [sequencing tasks with sbt-sequential](http://eed3si9n.com/sequencing-tasks-with-sbt-sequential) for details.

License
-------

MIT License. Copyright [@eed3si9n](https://twitter.com/eed3si9n) (Eugene Yokota).
