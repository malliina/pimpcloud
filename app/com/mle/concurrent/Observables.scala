package com.mle.concurrent

import rx.lang.scala.Observable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Promise}

/**
 * @author Michael
 */
object Observables {
  def after[T](duration: Duration)(code: => T): Future[T] = {
    val p = Promise[T]()
    lazy val codeEval = code
    val sub = observeAfter(duration).subscribe(_ => p trySuccess codeEval)
    val ret = p.future
    ret.onComplete(_ => sub.unsubscribe())
    ret
  }

  def observeAfter(duration: Duration) = Observable.interval(duration).take(1)
}
