package handle.response.sample1

import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorSystem

/**
 * 無名Acotr(= Extra)を使って応答を得るサンプル
 */
object ExtraPattern extends App {
  val system = ActorSystem()
  val foo = system.actorOf(Props[Foo])
  foo ! "foo"
}

class Foo extends Actor {
  val bar = context.actorOf(Props[Bar])

  def receive = {
    case "foo" =>
      context.actorOf(Props(new Actor {
        bar ! "bar"
        def receive = {
          case "buz" =>
            println("ok!")
            context stop self // メモリリーク防止
        }
        
        override def postStop() = {
          context.system.shutdown()
        }
      }))
  }
}

class Bar extends Actor {
  def receive = {
    case "bar" =>
      sender ! "buz"
  }
}
