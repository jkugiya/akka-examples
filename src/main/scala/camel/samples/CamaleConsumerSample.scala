package camel.samples

import java.io.InputStream

import scala.io.Source

import org.apache.camel.util.URISupport

import akka.actor.{ActorSystem, Props, actorRef2Scala}
import akka.camel.{CamelMessage, Consumer}

/**
 * 
 */
object CamaleConsumerSample extends App {
  val system = ActorSystem()
  val endpoint = system.actorOf(Props[MyEndpoint])

  class MyEndpoint extends Consumer {
    def endpointUri = "netty-http:http://localhost:8080/example"
    def receive = {
      case cm @ CamelMessage(body, headers) =>
        println(s"---- Headers ----")
        headers.foreach { case (key, value) =>
          println(s"${key}: ${value}")
        }
        println(s"---- End Headers ----")
        println(s"---- Htp Queries ----")
        println(URISupport.parseQuery(headers("CamelHttpQuery").asInstanceOf[String]))
        println(s"---- End Queries ----")
        println(s"---- Body ----")
        val source = Source.fromInputStream(body.asInstanceOf[InputStream])
        source.getLines().foreach { line =>
          println(line)
        }
        println(s"---- End ----")
        sender ! "ok"
    }
  }
}