package com.malliina.play

import akka.NotUsed
import akka.stream.scaladsl.{Keep, Sink, Source, SourceQueue}
import akka.stream.{Materializer, OverflowStrategy}
import play.api.libs.iteratee._

import scala.concurrent.ExecutionContext

object Streaming {
  val DefaultBufferSize = 10000000

  def arrayBuffer[T](size: Int)(implicit ec: ExecutionContext): Enumeratee[Array[T], Array[T]] =
    Concurrent.buffer[Array[T]](size, (i: Input[Array[T]]) => inputLength(i))

  def inputLength[T](in: Input[Array[T]]): Int = in match {
    case Input.El(e) => e.length
    case Input.Empty => 1
    case Input.EOF => 1
  }

  /** Builds a [[SourceQueue]] and a [[Source]], so that elements offered to the [[SourceQueue]]
    * will be emitted by the [[Source]].
    *
    * @param mat materializer
    * @tparam T type of element
    * @return a [[SourceQueue]] and a [[Source]]
    */
  def sourceQueue[T](mat: Materializer): (SourceQueue[Option[T]], Source[T, NotUsed]) = {
    val source = Source.queue[Option[T]](DefaultBufferSize, OverflowStrategy.backpressure)
      .takeWhile(_.isDefined).map(_.get)
    val (queue, publisher) = source.toMat(Sink.asPublisher(fanout = true))(Keep.both).run()(mat)
    val src = Source.fromPublisher(publisher)
    (queue, src)
  }
}
