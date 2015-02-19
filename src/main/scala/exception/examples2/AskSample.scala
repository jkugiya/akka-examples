package exception.examples2

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy, actorRef2Scala}
import akka.pattern.ask
import akka.util.Timeout

/**
 * アクターで例外が発生した時のサンプル。
 * 子アクターで発生した例外はSupervisorで面倒を見るというモデル。
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
  private val decider: SupervisorStrategy.Decider = {
    case cause =>
      println("Some Error")
      println(cause)
      SupervisorStrategy.Escalate
  }

  override val supervisorStrategy = OneForOneStrategy()(decider orElse SupervisorStrategy.defaultDecider)

  def receive = {
    case "foo" =>
      import context.dispatcher
      val crasher = context.actorOf(Crasher.props())
      val future = crasher ? "foo"
      future.onComplete {
        case Success(s) =>
          println("success")
        case Failure(t) =>
          // ここはタイムアウトくらいしかこない
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
    } catch {
      case cause: Throwable =>
        throw new CrasherException(sender, cause)
    }
  }
}

case class CrasherException(sender: ActorRef, cause: Throwable) extends Exception