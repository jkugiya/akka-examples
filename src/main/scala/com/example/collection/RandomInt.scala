package com.example.collection

import scala.collection.Iterator
import scala.util.Random

/**
 * ランダムな数字の無限数列
 */
class RandomInt extends Iterator[Int] {
  def hasNext: Boolean = true
  def next(): Int = Random.nextInt(100)
}