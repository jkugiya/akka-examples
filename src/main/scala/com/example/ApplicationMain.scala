//package com.example
//
//import akka.actor.ActorSystem
//import com.example.collection.RandomInt
//import akka.stream.scaladsl.Source
//import akka.stream.scaladsl.Sink
//import akka.stream.scaladsl.ForeachSink
//import akka.stream.FlowMaterializer
//import akka.stream.MaterializerSettings
//import akka.stream.scaladsl.Flow
//import akka.stream.scaladsl.FlowGraph
//import akka.stream.scaladsl.Merge
//import akka.stream.scaladsl.ForeachSink
//import akka.stream.scaladsl.FoldSink
//import akka.stream.scaladsl.FoldSink
//import akka.stream.scaladsl.Broadcast
//import akka.stream.scaladsl.FlowGraphImplicits._
//
//object ApplicationMain extends App {
//  implicit val system = ActorSystem("Stream")
//  val settings = MaterializerSettings(system)
//  implicit val mate = FlowMaterializer(settings)
//
//  println("------30回printする------")
//  val randomIntStream = Stream.continually(new RandomInt)
//  val source = Source(randomIntStream.take(30))
//  val printSink = ForeachSink { r:RandomInt => val n = r.next(); println(s"PrintSample: ${n}") }
//  source.runWith(printSink)
//
//  println("------Mapサンプル - 入力された値を10倍する------")
//  val printSink2 = ForeachSink { a:Any => println(s"MapSample: ${a}") }
//  source.map(_.next() * 10).runWith(printSink2)
//
//  println("------Mapサンプル - Flowをつかう------")
//  val flowSink = ForeachSink { a:Any => println(s"FlowMapSample: ${a}") }
//  val flowMap = Flow[RandomInt].map( r => r.next())
//  flowMap.runWith(source, flowSink)
//  
//  println("------Filterサンプル - 10より小さい値をフィルタする------")
//  val printSink3 = ForeachSink { a:Any => println(s"FilterSample: ${a}") }
//  source.map(_.next()).filter(_ >= 10).runWith(printSink2)
//  
//  println("------ FlowGraphサンプル - FlowGraphを使って、複雑なストリームを処理する。")
//  // 入力はRandomIntで50より大きい入力と50以下の入力に分割する。
//  // 分割したストリームをそれぞれprintする
//  // 後で分割ストリームをMergeしてprintする
//  // 最後に入力された値を合計した値をprintする
//  val printLarge = ForeachSink { n:Int => println(s"Large: ${n}")}
//  val printLess = ForeachSink { n:Int => println(s"Less: ${n}")}
//  val printAll = ForeachSink { n:Int => println(s"All: ${n}")}
//  val printSum = ForeachSink { n:Int => println(s"Sum: ${n}")}
//  FlowGraph { implicit b =>
//    val  evalFlow = source.map(_.next) ~> Broadcast[Int]
//    val largerFlow = evalFlow ~> Flow[Int].filter(_ > 50)
//    largerFlow ~> printLarge
//    val lessFlow = evalFlow ~> Flow[Int].filter(_ <= 50)
//    lessFlow ~> printLess
//    val merge = Merge[Int]
//    largerFlow ~> merge
//    lessFlow ~> merge
//    merge ~> Flow[Int].grouped(30).map(_.fold(0)(_+_)) ~> printSum
//  }.run()
//}