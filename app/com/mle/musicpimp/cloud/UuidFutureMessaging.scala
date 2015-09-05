package com.mle.musicpimp.cloud

import java.util.UUID

import com.mle.concurrent.Observables
import com.mle.util.Log

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Promise}

/**
 * @author Michael
 */
trait UuidFutureMessaging[T] extends FutureMessaging[T] with Log {
  val ongoing = TrieMap.empty[UUID, Promise[T]]

  def build(pair: BodyAndId): T

  def extract(response: T): Option[BodyAndId]

  def isSuccess(response: T): Boolean = true

  override def request(message: T, timeout: Duration): Future[T] = {
    // generates UUID for this request-response pair
    val uuid = UUID.randomUUID()
    val responsePromise = Promise[T]()
    ongoing += (uuid -> responsePromise)
    // sends the payload, including a request ID
    val payload = build(BodyAndId(message, uuid))
//    log info s"Sending: $payload"
    send(payload).recover {
      case t: Throwable =>
        log.warn(s"Unable to send payload: $payload", t)
        failExceptionally(uuid, t)
    }
    val task = responsePromise.future
    // fails promise after timeout
    if (!responsePromise.isCompleted) {
      Observables.after(timeout) {
        ongoing -= uuid
        if (!responsePromise.isCompleted) {
          val message = s"Request: $uuid timed out after: $timeout."
          val failed = responsePromise tryFailure new concurrent.TimeoutException(message)
          if (failed) {
            log warn message
          }
        }
      }
    }
    task
  }

  def complete(response: T): Boolean = {
    extract(response).exists(pair => {
      if (isSuccess(response)) succeed(pair.uuid, pair.body)
      else fail(pair.uuid, pair.body)
    })
  }

  /**
   * Completes the ongoing [[Promise]] identified by `requestID` with `responseBody`.
   *
   * @param requestID the request ID
   * @param responseBody the payload of the response, that is, the 'body' JSON value
   * @return true if an ongoing request with ID `requestID` existed, false otherwise
   */
  def succeed(requestID: UUID, responseBody: T): Boolean = baseComplete(requestID)(_.trySuccess(responseBody))

  /**
   * Fails the ongoing [[Promise]] identified by `requestID` with a [[RequestFailure]] containing `responseBody`.
   *
   * @param requestID request ID
   * @param responseBody body of failed response
   * @return true if an ongoing request with ID `requestID` existed, false otherwise
   */
  def fail(requestID: UUID, responseBody: T) = failExceptionally(requestID, new RequestFailure(responseBody))

  def failExceptionally(requestID: UUID, t: Throwable) = baseComplete(requestID)(_.tryFailure(t))

  private def baseComplete(request: UUID)(f: Promise[T] => Unit) = {
    (ongoing get request).exists(promise => {
      f(promise)
      ongoing -= request
      true
    })
  }

  class RequestFailure(val response: T) extends Exception

  case class BodyAndId(body: T, uuid: UUID)

}
