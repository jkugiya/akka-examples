package leak.sample

import java.lang.management.ManagementFactory

import akka.actor.{Actor, ActorSystem, Props}

object FactoryActorApp extends App {
  val system = ActorSystem()
  println("Creating instances.")
  for (i <- 1 to 100000) {
    system.actorOf(Props[SourceFActor]) ! "foo"
  }
  val pid = ManagementFactory.getRuntimeMXBean.getName.split('@').head
  println("Created instances.")
  println(s"jmap -histo:live ${pid}")
}

class SourceFActor extends Actor {
  val i = 1
  
  def receive = {
    case "foo" =>
      context.system.actorOf(TargetFActor.props(i))
      context.stop(self)
  }
}

object TargetFActor {
  def props(n: Int) = Props(classOf[TargetFActor], n)
}

class TargetFActor(n: Int) extends Actor {
  def receive = {
    case "bar" => println(n)
  }
}