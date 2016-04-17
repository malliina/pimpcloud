package com.malliina.play

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Source, SourceQueue}
import play.api.libs.iteratee.{Concurrent, Enumerator}
import rx.lang.scala.{Observable, Observer, Subscription}

import scala.concurrent.ExecutionContext

object StreamConversions {
  val DefaultBufferSize = 10000000

  /**
    *
    * @param obs source data
    * @tparam T type of data
    * @return a one-shot enumerator
    * @see http://bryangilbert.com/blog/2013/11/03/rx-the-importance-of-honoring-unsubscribe/
    */
  def fromObservableOneShot[T](obs: Observable[T])(implicit ec: ExecutionContext): Enumerator[T] = {
    var subscription: Option[Subscription] = None
    Concurrent.unicast[T](
      chan => subscription = Some(obs.subscribe(new ChannelObserver(chan))),
      subscription.foreach(_.unsubscribe()),
      (_, _) => subscription.foreach(_.unsubscribe()))
  }

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
