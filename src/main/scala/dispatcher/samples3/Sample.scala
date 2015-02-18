package dispatcher.samples3

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
import akka.routing.RoundRobinPool

/**
 * 全体で200スレッド。詰まる。
 */
object Sample extends App {
  
  val confStr = """
  akka {
    actor {
      fork-join-executor {
        parallelism-factor = 200
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
  val global = Global(system)
  val firstTaskExecutor = global.firstTaskExecutor
  import system.dispatcher
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

object Global {
  def apply(system: ActorSystem) = new Global(system) 
}

class Global(system: ActorSystem) {
  
  val thirdTaskExecutor = system.actorOf(RoundRobinPool(100).props(ThirdTaskExecutor.props()))
      
  val secondTaskExecutor = system.actorOf(RoundRobinPool(100).props(SecondTaskExecutor.props(thirdTaskExecutor)))
      
  val firstTaskExecutor = system.actorOf(RoundRobinPool(4).props(Props(classOf[FirstTaskExecutor], secondTaskExecutor)), "1st")
}

case class DoTask(startFrom: Long, queue: Queue[String])
case class End(executionTime: Long, queue: Queue[String])
case object Finished

class FirstTaskExecutor(secondTaskExecutor: ActorRef) extends Actor with ActorLogging {
  def receive = {
    case DoTask(startFrom, queue) =>
      val currentQueue = queue :+ s"First@[${Thread.currentThread().getId}]"
      log.info(s"First started. ${currentQueue}")
      val originalSender = sender
      context.actorOf(Props(new Actor {
        secondTaskExecutor ! DoTask(startFrom, currentQueue)
        def receive = {
          case e:End  =>
            originalSender ! e
        }
      }))
      log.info(s"First ended. ${currentQueue}")
  }
}

object SecondTaskExecutor {
  def props(thirdTaskExecutor: ActorRef) = Props(classOf[SecondTaskExecutor], thirdTaskExecutor)
}

class SecondTaskExecutor(thirdTaskExecutor: ActorRef) extends Actor with ActorLogging {
  def receive = {
    case DoTask(startFrom, queue) =>
      val currentQueue = queue :+ s"Second@[${Thread.currentThread().getId}]"
      log.info(s"SecondTask started. ${currentQueue}")
      Thread.sleep(3000L)
      log.info(s"SecondTask ended. ${currentQueue}")
      val originalSender = sender
      context.actorOf(Props(new Actor {
        thirdTaskExecutor ! DoTask(startFrom, currentQueue)
        def receive = {
          case e:End =>
            originalSender ! e
            context stop self
        }
      }))
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
  }
}
