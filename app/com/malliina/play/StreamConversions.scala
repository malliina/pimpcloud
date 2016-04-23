package com.malliina.play

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Source, SourceQueue}
import rx.lang.scala.{Observable, Observer}

object StreamConversions {
  def observableToSource[T](obs: Observable[T], mat: Materializer): Source[T, NotUsed] = {
    val (queue, source) = Streaming.sourceQueue[T](mat)
    val _ = obs.subscribe(new QueueObserver[T](queue))
    source
  }

  class QueueObserver[T](queue: SourceQueue[Option[T]]) extends Observer[T] {
    override def onNext(elem: T): Unit = queue.offer(Option(elem))

    override def onCompleted(): Unit = queue.offer(None)

    override def onError(e: Throwable): Unit = queue.offer(None) // ???
  }

}
