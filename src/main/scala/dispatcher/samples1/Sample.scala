package dispatcher.samples1

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Actor
import scala.collection.immutable.Queue
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.pattern.ask
import akka.actor.Props
import akka.routing.RoundRobinPool
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Future
import akka.actor.actorRef2Scala

/**
 * SecondExeutorのリークは解消したが、まだ10秒以上かかるタスクがある。
 */
object Sample extends App {
  
  val confStr = """
  akka {
    loglevel = "INFO"
    log-dead-letters-during-shutdown = off
  }
  my-dispatcher {
    type = Dispatcher
    executor = "fork-join-executor"
    fork-join-executor {
      parallelism-factor = 10
      parallelism-max = 80
      parallelism-min = 80
    }
  }
"""
  val conf = ConfigFactory.load(ConfigFactory.parseString(confStr))
  val system = ActorSystem("sample", conf)
  import system.dispatcher
  val firstTaskExecutor = system.actorOf(RoundRobinPool(50).props(Props[FirstTaskExecutor]).withDispatcher("my-dispatcher"), "1st")
  implicit val timeout = Timeout(60.seconds)
  /*
   * First->Second->Thirdの順にActorにメッセージを送る。
   */
  val futures = for (i <-1 to 50) yield {
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
  def receive = {
    case DoTask(startFrom, queue) =>
      val currentQueue = queue :+ s"First@[${Thread.currentThread().getId}]"
      val secondTaskExecutor = context.actorOf(SecondTaskExecutor.props(sender))
      log.info(s"First started. ${currentQueue}")
      secondTaskExecutor ! DoTask(startFrom, currentQueue)
      log.info(s"First ended. ${currentQueue}")
  }
}

object SecondTaskExecutor {
  def props(originalSender: ActorRef) = Props(classOf[SecondTaskExecutor], originalSender)
}

class SecondTaskExecutor(originalSender: ActorRef) extends Actor with ActorLogging {
  def receive = {
    case DoTask(startFrom, queue) =>
      val currentQueue = queue :+ s"Second@[${Thread.currentThread().getId}]"
      val thirdTaskExecutor = context.actorOf(ThirdTaskExecutor.props(originalSender))
      log.info(s"SecondTask started. ${currentQueue}")
      Thread.sleep(3000L)
      log.info(s"SecondTask ended. ${currentQueue}")
      thirdTaskExecutor ! DoTask(startFrom, currentQueue)
    case Finished =>
      context stop self
  }
}


object ThirdTaskExecutor {
  def props(originalSender: ActorRef) = Props(classOf[ThirdTaskExecutor], originalSender)
}

class ThirdTaskExecutor(originalSender: ActorRef) extends Actor with ActorLogging {
  def receive = {
    case DoTask(startFrom, queue) =>
      val currentQueue = queue :+ s"Third@[${Thread.currentThread().getId}]"
      log.info(s"ThirdTask started. ${currentQueue}")
      Thread.sleep(100L)
      log.info(s"ThirdTask ended. ${currentQueue}")
      Thread.sleep(100L)
      originalSender ! End(System.currentTimeMillis() - startFrom, currentQueue)
      sender ! Finished
  }
}
