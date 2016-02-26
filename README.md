sbt-sequential
==============

sbt-sequential adds sequential tasks to sbt.

**NOTE:**

[0.13.8-notes]: http://www.scala-sbt.org/0.13/docs/sbt-0.13-Tech-Previews.html#Sequential+tasks
[Sequencing]: http://www.scala-sbt.org/0.13/docs/Howto-Sequencing.html

While this plugin provides a macro that allows you to code in imperative-looking code, a weaker version of it (essentially a list of keys) was add to sbt, as of version 0.13.8.

See the relevant [release notes][0.13.8-notes] as well as [Sequencing][] section in the docs.

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
