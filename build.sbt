name := """akka-examples"""

version := "1.0"

scalaVersion := "2.11.5"	

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.9",
  "com.typesafe.akka" %% "akka-cluster" % "2.3.9",
  "com.typesafe.akka" %% "akka-contrib" % "2.3.9",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.9" % "test",
  "com.typesafe.akka" % "akka-stream-experimental_2.11" % "1.0-M3",
  "com.typesafe.akka" % "akka-http-core-experimental_2.11" % "1.0-M3",
  "io.spray" %%  "spray-json" % "1.3.1",
  "org.scala-lang.modules" %% "scala-async" % "0.9.2",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test")
  