//package com.example
//
//import akka.actor.ActorSystem
//import akka.http.model.HttpRequest
//import akka.http.model.HttpResponse
//import akka.http.model.HttpMethods._
//import akka.http.model.Uri
//import akka.http.Http
//import akka.stream.FlowMaterializer
//import scala.util.matching.Regex
//
//object HttpServerSample extends App {
//  implicit val system = ActorSystem()
//  import system.dispatcher
//  implicit val flowMaterializer = FlowMaterializer()
//  val hoge = "a(.*?)b"r
//  val fuga: Regex => String = { case hoge(ccc) => ccc.mkString }
//  type F = PartialFunction[HttpRequest, HttpResponse]
//  val a:F = {
//    case HttpRequest(GET, Uri.Path(""), _, _, _) => ???
//  }
//  val serverBinding = Http(system).bind(interface = "localhost", port = 8080)
//  serverBinding.connections foreach { connection =>
//    connection handleWithSyncHandler a
//  }
//}
//
//object DSL {
////  context(system) {
////    Get("") {
////      
////    },
////    Get
////    Post("") {
////      
////    }
////  }.bind("")
//}
//
