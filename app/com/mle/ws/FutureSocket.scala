package com.mle.ws

import java.util.UUID

import com.mle.concurrent.Observables
import com.mle.musicpimp.json.JsonStrings._
import com.mle.util.{Log, Utils}
import com.mle.ws.FutureSocket.ResponseException
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json._

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, DurationLong}
import scala.concurrent.{Future, Promise}
import scala.util.Try

/**
 * Emulates an HTTP client using a WebSocket channel. Supports timeouts.
 *
 * Protocol: We assume that responses will be tagged with the same request ID we add to sent messages, so that we can
 * pair requests with responses.
 *
 * @author Michael
 */
class FutureSocket(val channel: Channel[JsValue], val id: String) extends Log {
  val timeout = 20.seconds
  val ongoingFutures = trieMap[Promise, JsValue]

  // testing some syntax
  private def trieMap[T[_], U] = TrieMap.empty[UUID, T[U]]

  /**
   * TODO Fix signature; what happens when the channel is closed and this method is called?
   *
   * @param json payload
   */
  def send(json: JsValue) = Try(channel push json)

  /**
   * Sends `body` as JSON and deserializes the response to `U`.
   *
   * @param body message payload
   * @param writer json serializer
   * @param reader json deserializer
   * @tparam T type of request payload
   * @tparam U type of response
   * @return the response
   */
  def proxyT[T, U](body: T)(implicit writer: Writes[T], reader: Reads[U]): Future[U] = proxyD(writer writes body)

  /**
   * Sends `body` and deserializes the response to type `T`.
   *
   * @param body payload
   * @param reader json deserializer
   * @tparam T type of response
   * @return a deserialized body, or a failed [[Future]] on failure
   */
  def proxyD[T](body: JsValue)(implicit reader: Reads[T]): Future[T] = proxy(body).map(_.as[T])

  def proxyD2[T](body: JsValue)(implicit reader: Reads[T]): Future[JsResult[T]] = proxy(body).map(_.validate[T])

  //  def proxyDinTermsOfValidate[T](body: JsValue)(implicit reader: Reads[T]): Future[JsResult[T]] =
  //    proxy(body).map(_.validate[T]).map(_.fold(???, identity))

  def proxy(message: JsValue, timeout: Duration = timeout): Future[JsValue] = {
    // generates UUID for this request-response pair
    val uuid = UUID.randomUUID()
    val responsePromise = Promise[JsValue]()
    ongoingFutures += (uuid -> responsePromise)
    // sends the payload, including a request ID
    val payload = Json.obj(REQUEST_ID -> uuid.toString, BODY -> message)
    send(payload).recover {
      case t: Throwable =>
        log.warn(s"Unable to send payload: $payload", t)
        fail(uuid, t)
    }
    val task = responsePromise.future
    // remove after timeout if not already removed
    if (!responsePromise.isCompleted) {
      Observables.after(timeout) {
        ongoingFutures -= uuid
        if (!responsePromise.isCompleted) {
          val message = s"Request: $uuid timed out after: $timeout."
          responsePromise tryFailure new concurrent.TimeoutException(message)
          log warn message
        }
      }
    }
    task
  }

  def complete(response: JsValue): Boolean = {
    val uuidResult = (response \ REQUEST_ID).validate[UUID]
    uuidResult.map(requestID => complete(requestID, response \ BODY)).filter(_ == true).isSuccess
  }

  /**
   * Completes the ongoing [[Promise]] identified by `requestID` with `responseBody`.
   *
   * @param requestID the request ID
   * @param responseBody the payload of the response, that is, the 'body' JSON value
   * @return true if an ongoing request with ID `requestID` existed, false otherwise
   */
  def complete(requestID: UUID, responseBody: JsValue): Boolean = baseComplete(requestID)(_.trySuccess(responseBody))

  def fail(requestID: UUID, t: Throwable) = baseComplete(requestID)(_.tryFailure(t))

  def failWithMessage(request: UUID, message: JsValue) = fail(request, new ResponseException(message))

  private def baseComplete(request: UUID)(f: Promise[JsValue] => Unit) = {
    (ongoingFutures get request).exists(promise => {
      f(promise)
      ongoingFutures -= request
      true
    })
  }

  //  def completeLogged(requestID: String, response: JsValue) = {
  //    val ret = complete(requestID, response)
  //    ret.fold(log.warn(s"Invalid request ID: $requestID"))(isSuccess => {
  //      if (isSuccess) log info s"Completed request: $requestID with value: $response"
  //      else log info s"Unable to find channel with request ID: $requestID, cannot complete value: $response"
  //    })
  //    ret
  //  }
}

object FutureSocket {

  class ResponseException(val response: JsValue) extends Exception

  def tryParseUUID(id: String): Option[UUID] = Utils.opt[UUID, IllegalArgumentException](UUID.fromString(id))
}
