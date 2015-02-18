package com.example

import com.typesafe.config.ConfigFactory
import akka.actor.{Actor, ActorLogging, ActorPath, ActorSystem, Props, actorRef2Scala}
import akka.contrib.pattern.{ClusterClient, ClusterReceptionistExtension}

object BiCommunicateCluster extends App {
  val Array(host, port, systemName, targetHost, targetPort, targetName, _*) = args
  val confText = s"""
akka {
  actor {
    provider = "akka.cluster.ClusterActorRefProvider"
  }
  cluster {
  //  seed-nodes = []
    seed-nodes = ["akka.tcp://${systemName}@${host}:${port}"]
  }
  extensions = ["akka.contrib.pattern.ClusterReceptionistExtension"]
  remote {
    log-remote-lifecycle-events = off
    netty.tcp {
      hostname = ${host}
      port = ${port}
    }
  }
  contrib.cluster.client {
    mailbox {
      mailbox-type = "akka.dispatch.UnboundedDequeBasedMailbox"
      stash-capacity = 1000
    }
  }
  akka.contrib.cluster.receptionist {
    name = receptionist
    number-of-contacts = 3
    response-tunnel-receive-timeout = 30s
  }
  // loglevel = "DEBUG"
}
"""
  val conf = ConfigFactory.load(ConfigFactory.parseString(confText))
  val system = ActorSystem(systemName, conf)
  // 相手側
  val targetPath = s"akka.tcp://${targetName}@${targetHost}:${targetPort}/user/receptionist"
  val pinger = system.actorOf(Pinger.props(targetPath), "ping")
  val ponger = system.actorOf(Ponger.props(targetPath), "pong")
  ClusterReceptionistExtension(system).registerService(ponger)// Pongは公開する
  Thread.sleep(10000L)
  pinger ! Protocols.SendPing
}

/**
 * メッセージ
 */
object Protocols {
  case object SendPing
  case class Ping(path: ActorPath)
  case class Pong(path: ActorPath)
}

// Ping
object Pinger {
  def props(targetAddress: String) = Props(classOf[Pinger], targetAddress)
}

class Pinger(receptionistAddress: String) extends Actor with ActorLogging {
  import Protocols._
  log.info(s"Pinger will send to ${context.actorSelection(receptionistAddress)}")
  val client = context.actorOf(ClusterClient.props(initialContacts =
    Set(receptionistAddress).map(context.actorSelection(_))))
  
  def receive = {
    case SendPing =>
      log.info("Received SendPing")
      val pongReceiver = context.actorOf(Props[PongReceiver])
      log.info(s"Pong Receiver's path = ${pongReceiver.path}")
      ClusterReceptionistExtension(context.system).registerService(pongReceiver)
      client ! ClusterClient.Send("/user/pong", Ping(pongReceiver.path), false)
  }
}

// Pong
object Ponger {
  def props(receptionistAddress: String) = Props(classOf[Ponger], receptionistAddress)
}

class Ponger(receptionistAddress: String) extends Actor with ActorLogging {
  import Protocols._
  log.info(s"Ponger will respond to ${context.actorSelection(receptionistAddress)}")
  val client = context.actorOf(ClusterClient.props(initialContacts =
    Set(receptionistAddress).map(context.actorSelection(_))))

  def receive = {
    case Ping(path) =>
      log.info(s"Ping from ${path}.")
      client ! ClusterClient.Send(path.toStringWithoutAddress, Pong(self.path), false)
  }
}

// PingerがPingしてからPongを受け取る人
// メッセージを受け取ったら死ぬ
// メッセージの受信をPingとは並列にしたいため、別Actorにする。
class PongReceiver extends Actor with ActorLogging {
  import Protocols._
  def receive = {
    case Pong(address) =>
      log.info(s"Pong from ${address}")
      context stop self
  }
}
