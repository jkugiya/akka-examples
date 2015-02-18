package com.example

import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout

object HandleMessageSample extends App {
  val system = ActorSystem.create()
  val printer = system.actorOf(Props[Printer])
  
  printer ! "echo"
  printer ! "echo"
  printer ! "echo"
  printer ! "echo"
  printer ! "echo"
  printer ! "echo"
  println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")

}

class Printer extends Actor {
  import context.dispatcher
  val echor = context.actorOf(Props[Echor])
  var i = 1
  var j = 100
  implicit val timeout: Timeout = 10 seconds

  def receive = {
    case "hello" =>
      println(s"${i}## Hello !!!!")
      Thread.sleep(3000L)
      println(s"${i}## World!!!!")
      i += 1
    case "echo" =>
     val f = echor ? j 
     f.mapTo[Int].foreach { x => println(s"${x}### Echo ###${j}") }
     j += 1
     println("echoed")
  }
}


class Echor extends Actor {
  def receive = {
    case s =>
      Thread.sleep(2000L)
      sender ! s
  }
}