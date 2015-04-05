package future.samples

import java.lang.management.ManagementFactory

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Random

object FutureSeqSample2 extends App {
  val result = calcurate(100000)
  val pid = ManagementFactory.getRuntimeMXBean.getName.split('@').head
  println(s"jmap -histo:live ${pid}")
  Thread.sleep(30000000L)
  
  def calcurate(n: Int): Int= {
    val futures = for (i <- 1 to n) yield {
      Future {
        Random.nextInt()
      }
    }
    val f = Future.sequence(futures)
    val result = Await.result(f, Duration.Inf)
    result.reduceLeft(_+_)
  }
}
