package future.samples

import java.lang.management.ManagementFactory

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.{Random, Success}

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Stash, actorRef2Scala}
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout

object ActorSeqSample extends App {
  val pid = ManagementFactory.getRuntimeMXBean.getName.split('@').head
  println(s"jmap -histo:live ${pid}")

  implicit val timeout = Timeout(100 seconds)
  val system = ActorSystem()
  import system.dispatcher
  
  val executor = system.actorOf(Props[Executor])
  val result = executor ? Start(100000)
  result.mapTo[CalculateResult] onComplete {
    case Success(sum) => println(s"sum is ${sum.n}")
    case _ => // このサンプルでは失敗しない
  }
  Await.result(result, Duration.Inf)
  Thread.sleep(300000L)
}

case class Start(n: Int)
case class CalculateResult(n: Int)

class Executor extends Actor with Stash {
  var sum = 0
  var resultCount = 0
  var maxCount = 0
  val randomIntActors = context.actorOf(new RoundRobinPool(10).props(Props[RandomIntActor]))
  
  def receive: Actor.Receive = {
    case Start(n) =>
      for (i <- 1 to n) {
        randomIntActors ! "do it"  
      }
      sum = 0
      resultCount = 0
      maxCount = n
      context become calculate(sender)
  }
  
  def calculate(originalSender: ActorRef): Actor.Receive = {
    case CalculateResult(n) =>
      println(n)
      resultCount += 1
      if (resultCount >= maxCount) {
        originalSender ! CalculateResult(sum)
        context.unbecome()
        unstashAll()
      }
    case _ => stash()
  }
}

class RandomIntActor extends Actor {
  def receive = {
    case msg =>
      Thread.sleep(3000000L)
      sender ! CalculateResult(Random.nextInt())
  }
}
