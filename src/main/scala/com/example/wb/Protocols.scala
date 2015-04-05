package com.example.wb

import akka.actor.Address

case class RequestWork(address: Address)

case class DoWork(work: Work)

case class CouldntDoWork(work: Work)

case object FinishedWork

case object NothingToDo

case class Work(id: Long, data: Option[Any]) extends Serializable