package initialization.sample

import akka.actor.{ActorSystem, Props, actorRef2Scala}
import akka.actor.Actor
import akka.actor.ActorSelection.toScala

/**
 * ActorRefを使ってメッセージを送る場合は、初期化を待ち合わせてくれる。
 * ActorSelectionは待ち合わせをしない。
 */
object IniSample extends App {
  val system = ActorSystem()
  println(s"Main Thread is ${Thread.currentThread().getName}")
  
  val actor = system.actorOf(Props[ClassA], "actor")
  val selection = system.actorSelection("/users/actor")

  actor ! "The World!"
  selection ! "Star Platinum!"

  println("Message was sended.")
  Thread.sleep(10000L)
  system.shutdown()
}


class ClassA extends Actor {
  println(s"Class A Thread is ${Thread.currentThread().getName}")
  Thread.sleep(3000L)
  
  def receive = {
    case msg =>
      println(s"Got message. message=${msg}")
  }
}