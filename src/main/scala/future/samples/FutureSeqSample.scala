package future.samples

import java.lang.management.ManagementFactory

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.concurrent.forkjoin.ForkJoinPool
import scala.util.Random

object FutureSeqSample extends App {
  val pid = ManagementFactory.getRuntimeMXBean.getName.split('@').head
  println(s"jmap -histo:live ${pid}")
  val forkJoinExecutor = new ForkJoinPool(10)
  implicit val ec = ExecutionContext.fromExecutor(forkJoinExecutor)

  val futures = for (i <- 1 to 100000) yield {
    Future {
      Thread.sleep(300000000L)
      Random.nextInt()
    }
  }
  val f = Future.sequence(futures)
  f onSuccess {
    case seq => println(s"sum is ${seq.reduceLeft(_+_)}")
  }
  Await.result(f, Duration.Inf)
  Thread.sleep(30000000L)
}
