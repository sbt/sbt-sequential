sbtPlugin := true

name := "sbt-sequential"

organization := "com.eed3si9n"

version := "0.1.0-SNAPSHOT"

description := "sbt plugin to create sequential tasks"

licenses := Seq("MIT License" -> url("http://opensource.org/licenses/MIT"))

scalacOptions := Seq("-deprecation", "-unchecked")

publishArtifact in (Compile, packageBin) := true

publishArtifact in (Test, packageBin) := false

publishArtifact in (Compile, packageDoc) := false

publishArtifact in (Compile, packageSrc) := true

publishMavenStyle := false

publishTo <<= (version) { version: String =>
   val scalasbt = "http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"
   val (name, u) = if (version.contains("-SNAPSHOT")) ("sbt-plugin-snapshots", scalasbt+"sbt-plugin-snapshots")
                   else ("sbt-plugin-releases", scalasbt+"sbt-plugin-releases")
   Some(Resolver.url(name, url(u))(Resolver.ivyStylePatterns))
}

credentials += Credentials(Path.userHome / ".ivy2" / ".sbtcredentials")

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
  val t1 = Def.task { startServer.value }
  val t2 = Def.taskDyn { val _ = t1.value; Def.task { n = numberTask.value } }
  val t3 = Def.taskDyn { val _ = t2.value; Def.task { Thread.sleep(1000) } }
  val t4 = Def.taskDyn { val _ = t3.value; Def.task { val x = stopServer.value } }
  Def.taskDyn { val _ = t4.value; Def.task { n } }
}

combinedInSeq := foo.value
