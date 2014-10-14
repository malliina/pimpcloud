package com.mle.play

import play.api.libs.iteratee.{Concurrent, Enumerator}
import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.ExecutionContext

/**
 * @author Michael
 */
object Enumerators {
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
}
