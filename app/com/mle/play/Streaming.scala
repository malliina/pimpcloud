package com.mle.play

import play.api.libs.iteratee._
import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.ExecutionContext

/**
 * @author Michael
 */
object Streaming {


  def arrayBuffer[T](size: Int)(implicit ec: ExecutionContext): Enumeratee[Array[T], Array[T]] =
    Concurrent.buffer[Array[T]](size, (i: Input[Array[T]]) => inputLength(i))

  def inputLength[T](in: Input[Array[T]]): Int = in match {
    case Input.El(e) => e.length
    case Input.Empty => 1
    case Input.EOF => 1
  }
}


