package leak.sample

import java.lang.management.ManagementFactory

import akka.actor.{Actor, ActorSystem, Props}

object NewActorApp extends App {
  val system = ActorSystem()
  println("Creating instances.")
  for (i <- 1 to 100000) {
    system.actorOf(Props[SourceNActor]) ! "foo"
  }
  val pid = ManagementFactory.getRuntimeMXBean.getName.split('@').head
  println("Created instances.")
  println(s"jmap -histo:live ${pid}")
}

class SourceNActor extends Actor {
  val i = 1
  
  def receive = {
    case "foo" =>
      context.system.actorOf(Props(new TargetNActor(i)))
      context.stop(self)
  }
}

class TargetNActor(n: Int) extends Actor {
  def receive = {
    case "bar" => println(n)
  }
}