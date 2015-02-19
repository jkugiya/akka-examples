package exception.examples3

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

import akka.actor.{Actor, ActorSystem, Props, actorRef2Scala}
import akka.pattern.ask
import akka.util.Timeout

/**
 * 成功と失敗の両方のメッセージを用意する。
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
        case Success(OK) =>
          println("success")
        case Success(CrasherException(cause)) =>
          println("Some Error")
          println(cause)
          context.system.shutdown() // とりあえず、プログラムを終わらせるため
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
    case "foo" => try {
        throw new RuntimeException("fail!")
        sender ! OK
      } catch {
        case cause:Throwable =>
          sender ! CrasherException(cause)
      }
  }
}
trait Protocol
case object OK extends Protocol
case class CrasherException(cause: Throwable) extends Protocol