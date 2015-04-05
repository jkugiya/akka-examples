package com.example.faulttolerance

import akka.actor.ActorSystem
import akka.actor.ActorDSL._
import akka.actor.Actor
import scala.concurrent.duration._
import akka.actor.ActorRef

object UpdateFieldWhenRestart extends App {
  val system = ActorSystem()

  // 親アクター
  val parent = actor(system, "parent")(new Act {

    superviseWith(OneForOneStrategy() {
      case e: DontMindException => Restart
      case _ => Stop
    })

    // 子アクター
    val firstFail: ActorRef = actor("counter")(new Act {

      var count = 0

      // 子のレシーバ
      become {
        case "hello" =>
          println(s"message received. self=${self}")
          if (count < 4) throw new DontMindException
          println("ended process")
          system.shutdown()
      }

      // preRestartと同じ
      whenFailing { (cause, msg) =>
        println("detected error")
        // Retry
        import context.dispatcher
        // 1秒後に自分のメールボックスにメッセージを入れなおす。
        val originalSender = sender
        context.system.scheduler.scheduleOnce(Duration(1, SECONDS)) {
          println(s"msg=${msg}")
          msg.foreach(firstFail.tell(_, originalSender))
        }
      }

      whenRestarted { cause =>
        count += 1
        println(s"count = ${count}")// => ずっと1
        println("restarted.")
      }
    })

    // 親のレシーバ
    become {
      case "go" =>
        // 子アクターに処理を委譲
        firstFail ! "hello"
    }
  })

  parent ! "go"
}