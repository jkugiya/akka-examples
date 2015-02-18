package dispatcher.samples2

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

/**
 * Future。比較的詰まってないが、10秒以上かかるタスクがある。
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
  val firstTaskExecutor = system.actorOf(RoundRobinPool(4).props(Props[FirstTaskExecutor]), "1st")
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
  import context.dispatcher
  implicit val timeout = Timeout(60.seconds)
  def receive = {
    case DoTask(startFrom, queue) =>
      val currentQueue = queue :+ s"First@[${Thread.currentThread().getId}]"
      val secondTaskExecutor = context.actorOf(SecondTaskExecutor.props())
      log.info(s"First started. ${currentQueue}")
      val originalSender = sender
      (secondTaskExecutor ? DoTask(startFrom, currentQueue)).onSuccess {
        case end =>
          originalSender ! end
      }
      log.info(s"First ended. ${currentQueue}")
  }
}

object SecondTaskExecutor {
  def props() = Props(classOf[SecondTaskExecutor])
}

class SecondTaskExecutor extends Actor with ActorLogging {
  import context.dispatcher
  implicit val timeout = Timeout(60.seconds)
  def receive = {
    case DoTask(startFrom, queue) =>
      val currentQueue = queue :+ s"Second@[${Thread.currentThread().getId}]"
      val thirdTaskExecutor = context.actorOf(ThirdTaskExecutor.props())
      log.info(s"SecondTask started. ${currentQueue}")
      Thread.sleep(3000L)
      log.info(s"SecondTask ended. ${currentQueue}")
      val originalSender = sender
      (thirdTaskExecutor ? DoTask(startFrom, currentQueue)).onSuccess {
        case end =>
          originalSender ! end
      }
    case Finished =>
      context stop self
  }
}


object ThirdTaskExecutor {
  def props() = Props(classOf[ThirdTaskExecutor])
}

class ThirdTaskExecutor extends Actor with ActorLogging {
  def receive = {
    case DoTask(startFrom, queue) =>
      val currentQueue = queue :+ s"Third@[${Thread.currentThread().getId}]"
      log.info(s"ThirdTask started. ${currentQueue}")
      Thread.sleep(100L)
      log.info(s"ThirdTask ended. ${currentQueue}")
      Thread.sleep(100L)
      sender ! End(System.currentTimeMillis() - startFrom, currentQueue)
      log.info(s"This sender's class is ${sender.getClass}")
  }
}
