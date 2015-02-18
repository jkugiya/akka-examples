package dispatcher.samples5

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Actor
import akka.pattern.ask
import scala.collection.immutable.Queue
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.routing.RoundRobinPool
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Future

/**
 * context stopで子供も殺してしまった。
 * 終わらない。
 */
object Sample extends App {
  
  val confStr = """
  akka {
    actor {
      fork-join-executor {
        parallelism-factor = 0.5
        parallelism-min = 4
        parallelism-max = 4
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
   * 1～4並列ぐらいなら問題なく終わるが、8並列するぐらいにすると処理が終了しなくなる。
   */
  val futures = for (i <-1 to 8) yield {
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
      context stop self
  }
}
