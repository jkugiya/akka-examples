package com.example.dsl

import akka.actor.ActorDSL._
import akka.actor.ActorSystem

object Demo extends App {
  implicit val system = ActorSystem("demo")
  
  val a = actor(new Act {
    // Life Cycle Methods
    whenStarting {
      // starting
    }
    whenStopping {
      // stopping
    }
    // Receiver
    become {
      case "hello" => sender ! "hi"
      case "hoge" => becomeStacked {
        case "1" => sender ! "1"
        case "2" => unbecome()
      }
    }
  })
}