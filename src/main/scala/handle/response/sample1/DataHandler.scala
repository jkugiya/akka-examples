package handle.response.sample1

import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorSystem

/**
 * Senderが自分でハンドラを持って処理するサンプル
 */
object DataHandler extends App {
  val system = ActorSystem()
  val fizz = system.actorOf(Props[Fizz])
  fizz ! "fizz"
}

class Fizz extends Actor {
  val buzz = context.actorOf(Props[Buzz])

  def receive = {
    case "fizz" =>
      context become handleData
      buzz ! "buzz"
  }

  def handleData: Actor.Receive = {
    case "fizzbuzz" =>
      println("ok")
      context.unbecome()
      context.system.shutdown()// サンプルなので終わらせる
  }
}

class Buzz extends Actor {
  def receive = {
    case "buzz" =>
      sender ! "fizzbuzz"
  }
}
