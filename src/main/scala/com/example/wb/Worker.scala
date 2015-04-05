package com.example.wb

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Stash
import scala.concurrent.duration._
import akka.actor.Props
import scala.concurrent.Future

object Worker {
  def props(boss: ActorRef) = Props(classOf[Worker], boss)
}

class Worker(boss: ActorRef) extends Actor {
  requestWork()
  
  def receive = {
    case DoWork(work) =>
      context become processing
      // do something
      val worker = self
      import context.dispatcher
      Future {
        Thread.sleep(3000L)
        worker ! FinishedWork
      }
    case NothingToDo => requestWork()
  }
      
  def requestWork() = {
    import context.dispatcher
    context.system.scheduler.schedule(0.seconds, 30.seconds, boss, RequestWork(self.path.address))
  }

  def processing: Actor.Receive = {
    case DoWork(work) =>
      sender ! CouldntDoWork(work)
    case FinishedWork =>
      context unbecome
    case _ =>
  }
}