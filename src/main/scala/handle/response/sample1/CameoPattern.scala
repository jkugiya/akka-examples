package handle.response.sample1

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.ActorSystem

/**
 * 名のあるActor(= Cameo)を使って応答を得るPattern
 */
object CameoPattern extends App {
  val system = ActorSystem()
  val hop = system.actorOf(Props[Hop])
  hop ! "hop"
}

object Cameo {
  def props(step: ActorRef):Props = Props(classOf[Cameo], step)
}

class Cameo(step: ActorRef) extends Actor {
  step ! "step"

  def receive  = {
    case "jump" =>
      println("ok")
      context stop self// メモリリーク防止
  }
  
  override def postStop() = {
    context.system.shutdown()
  }
}

class Hop extends Actor {
  val step = context.actorOf(Props[Step])
  def receive = {
    case "hop" =>
      context.actorOf(Cameo.props(step)) ! "step"
  }
}

class Step extends Actor {
  def receive = {
    case "step" =>
      sender ! "jump"
  }
}
