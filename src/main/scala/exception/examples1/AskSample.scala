package exception.examples1

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

import akka.actor.{Actor, ActorSystem, Props, actorRef2Scala}
import akka.pattern.ask
import akka.util.Timeout

/**
 * アクターで例外が発生した時のサンプル。
 * Crasherで発生した例外はParentに伝播しない
 */
object AskSample extends App {
  val system = ActorSystem()
  val parent = system.actorOf(Parent.props())
  parent ! "foo"
}

object Parent {
  def props() = Props[Parent]
}

class Parent extends Actor {
  implicit val timeout: Timeout = Timeout(10.seconds)

  def receive = {
    case "foo" =>
      import context.dispatcher
      val crasher = context.actorOf(Crasher.props())
      val future = crasher ? "foo"
      future.onComplete {
        case Success(s) =>
          println("success")
        case Failure(t) =>
          println("failure")
          println(t)
          Await.result(future, 10.seconds)
          context.system.shutdown() // とりあえず、プログラムを終わらせるため
      }
  }
}

object Crasher {
  def props() = Props[Crasher]
}

class Crasher extends Actor {
  def receive = {
    case "foo" =>
      throw new RuntimeException
  }
}
