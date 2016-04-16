package com.malliina.play

import play.api.libs.iteratee.Concurrent.Channel
import rx.lang.scala.Observer

class ChannelObserver[T](channel: Channel[T]) extends Observer[T] {
  override def onNext(elem: T): Unit = channel push elem

  override def onCompleted(): Unit = channel.end()

  override def onError(e: Throwable): Unit = channel.end(e)
}
