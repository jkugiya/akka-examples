package dispatcher.samples6

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Actor
import scala.collection.immutable.Queue
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Future
import akka.actor.actorRef2Scala
import scala.util.Success
import scala.concurrent.ExecutionContext
import scala.concurrent.forkjoin.ForkJoinPool

/**
 * Future。samples2とほぼ同じだが、Future作る前に実行コンテクストを切り替える。
 */
object Sample extends App {
  
  val confStr = """
  akka {
    actor {
      fork-join-executor {
        parallelism-factor = 60
        parallelism-min = 200
        parallelism-max = 200
      }
    }
    loglevel = "INFO"
    log-dead-letters-during-shutdown = off
  }
"""
  val conf = ConfigFactory.load(ConfigFactory.parseString(confStr))
  val system = ActorSystem("sample", conf)
  val firstTaskExecutor = system.actorOf(RoundRobinPool(200).props(Props[FirstTaskExecutor]), "1st")
  import system.dispatcher
  implicit val timeout = Timeout(60.seconds)
  /*
   * First->Second->Thirdの順にActorにメッセージを送る。
   */
  val futures = for (i <-1 to 30) yield {
    val taskName = s"Task(${"%03d".format(i)})"
    firstTaskExecutor ? DoTask(System.currentTimeMillis(), Queue.empty[String] :+ taskName)
  }
  Future.sequence(futures).onSuccess {
    case results =>
      results.foreach { case End(executionTime, queue) =>
        println(s"${queue} finished. executionTime=${executionTime}.")
      }
      system.shutdown()
  }
}

case class DoTask(startFrom: Long, queue: Queue[String])
case class End(executionTime: Long, queue: Queue[String])
case object Finished

class FirstTaskExecutor extends Actor with ActorLogging {
  implicit val timeout = Timeout(60.seconds)
  def receive = {
    case DoTask(startFrom, queue) =>
      var currentQueue = queue :+ s"First@[${Thread.currentThread().getId}]"
      log.info(s"First started. ${currentQueue}")
      val originalSender = sender
      implicit val ec = 
      Future {
        currentQueue = currentQueue :+ s"Second@[${Thread.currentThread().getId}]"
        log.info(s"SecondTask started. ${currentQueue}")
        Thread.sleep(3000L)
        log.info(s"SecondTask ended. ${currentQueue}")
        Future {
          currentQueue = currentQueue :+ s"Third@[${Thread.currentThread().getId}]"
          log.info(s"ThirdTask started. ${currentQueue}")
          Thread.sleep(200L)
          log.info(s"SecondTask ended. ${currentQueue}")
          originalSender ! End(System.currentTimeMillis() - startFrom, currentQueue)
        }(ExecutionContext.fromExecutor(new ForkJoinPool))
      }(ExecutionContext.fromExecutor(new ForkJoinPool))
      log.info(s"First ended. ${currentQueue}")
  }
}
