package com.example.faulttolerance

import akka.actor.ActorSystem
import akka.actor.ActorDSL._
import akka.actor.Actor
import scala.concurrent.duration._
import akka.actor.ActorRef

object RetryWhenRestart extends App {
  val system = ActorSystem()

  // 親アクター
  val parent = actor(system, "parent")(new Act {

    // 親アクターのSupervisorStrategy
    superviseWith(OneForOneStrategy() {
      // Supervisorによる指示はRestartしなさい、だけ。
      case e: DontMindException => Restart
      case _ => Stop
    })

    // 子アクター
    val firstFail: ActorRef = actor("firstFail")(new Act {

      var isError = true

      // 子のレシーバ
      become {
        case "hello" =>
          println(s"message received. self=${self}")
          if (isError) throw new DontMindException
          println("ended process")
          system.shutdown()
      }

      // preRestartと同じ
      whenFailing { (cause, msg) =>
        println("detected error")
        // Retry
        import context.dispatcher
        // 2秒後に自分のメールボックスにメッセージを入れなおす。
        val originalSender = sender
        context.system.scheduler.scheduleOnce(Duration(2, SECONDS)) {
          println(s"msg=${msg}")
          msg.foreach(firstFail.tell(_, originalSender))
        }
      }

      whenRestarted { cause =>
        isError = false
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

