package com.example.wb

import akka.actor.Actor
import scala.util.Random
import akka.actor.Props

object Boss {
  def props()(implicit workRepository: WorkRepository = MockWorkRepository) = Props(classOf[Boss], workRepository)
}

class Boss(workRepository: WorkRepository) extends Actor {

  def receive = {
    case RequestWork(address) =>
      workRepository.findWorkEnableWork() match {
        case Some(work) =>
          sender ! DoWork(work)
        case None =>
          sender ! NothingToDo
      }
    case CouldntDoWork(work) =>
      workRepository.saveWork(work)
  }
}

trait WorkRepository {
  def findWorkEnableWork(): Option[Work]
  def saveWork(work: Work): Option[Long]
}

object MockWorkRepository extends WorkRepository {
  def findWorkEnableWork(): Option[Work] = {
    (Random.nextLong(), Random.nextInt()) match {
      case (l, i) if l < 0 => None
      case (l, i) if i < 0 => Some(Work(l, None))
      case (l, i) => Some(Work(l, Some(i)))
    }
  }
  
  def saveWork(work: Work): Option[Long] = {
    Some(work.id)
  }
}
