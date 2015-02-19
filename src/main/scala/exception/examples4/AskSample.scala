package exception.examples4

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

import akka.actor.{Actor, ActorSystem, Props, actorRef2Scala}
import akka.util.Timeout

/**
 * Acotr①->Actor②でaskするならFutureでいいのではないか。
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
      import context.dispatcher// Futureの内容に応じてdispatcherを変える。
      val future = Crasher.logic()
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
  def logic()(implicit ec: ExecutionContext) = Future {
    throw new RuntimeException
  }
}
