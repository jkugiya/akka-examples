package com.example

import java.io.RandomAccessFile

// RandomAccessFileを使ってファイル分割したStreamを取るサンプル
// 遅い。。。
object FileSplit extends App {
  val byLine = 10
  
  val lineseqs = getLinesStreams("30line-text.txt", 10)
  println(lineseqs.size)
  (lineseqs zipWithIndex) foreach {case (seq, i) =>
    println(s"-------------- ${i} -----------------")
    seq.foreach(println)
  }
  def getLinesStreams(fileName: String, bylines: Int):List[Stream[String]] = {
    @scala.annotation.tailrec
    def internal(lineCount: Int, from: Long, file: RandomAccessFile, result: List[Stream[String]]): List[Stream[String]] = {
      if (file.readLine() == null) result.reverse
      else if (lineCount >= bylines - 1) {
        val newFile = new RandomAccessFile(fileName, "r")
        newFile.seek(file.getFilePointer)
        internal(0, newFile.getFilePointer,
            newFile,
            createLineStream(from, newFile.getFilePointer, file)::result)
      } else {
        internal(lineCount + 1, from, file, result)
      }
    }
    internal(0, 0L, new RandomAccessFile(fileName, "r"), Nil)
  }
  
  def createLineStream(from: Long, to: Long, rf: RandomAccessFile): Stream[String] = try {
    rf.seek(from)
    val line = rf.readLine()
    if (line == null || rf.getFilePointer > to) {
      rf.close()
      Stream.empty
    } else {
      Stream.cons(line, createLineStream(rf.getFilePointer, to, rf))
    }
  } catch {
    case t: Throwable =>
      rf.close()
      throw t
  }
}

