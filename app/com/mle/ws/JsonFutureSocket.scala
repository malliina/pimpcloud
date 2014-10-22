package com.mle.ws

import java.util.UUID

import com.mle.musicpimp.cloud.UuidFutureMessaging
import com.mle.musicpimp.json.JsonStrings._
import com.mle.util.{Log, Utils}
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.util.Try

/**
 * Emulates an HTTP client using a WebSocket channel. Supports timeouts.
 *
 * Protocol: We assume that responses will be tagged with the same request ID we add to sent messages, so that we can
 * pair requests with responses.
 *
 * @author Michael
 */
class JsonFutureSocket(val channel: Channel[JsValue], val id: String) extends UuidFutureMessaging[JsValue] with Log {
  val timeout = 20.seconds
  // testing some syntax
  //  private def trieMap[T[_], U] = TrieMap.empty[UUID, T[U]]

  override def build(pair: BodyAndId) = Json.obj(REQUEST_ID -> pair.uuid.toString) ++ pair.body.as[JsObject]

  override def extract(response: JsValue): Option[BodyAndId] = {
    val uuidResult = (response \ REQUEST_ID).asOpt[UUID]
    uuidResult.map(req => BodyAndId(response \ BODY, req))
  }

  override def isSuccess(response: JsValue): Boolean = (response \ SUCCESS).validate[Boolean].filter(_ == false).isError

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
   * TODO check success status first, and any potential error
   *
   * @param body payload
   * @param reader json deserializer
   * @tparam T type of response
   * @return a deserialized body, or a failed [[Future]] on failure
   */
  def proxyD[T](body: JsValue)(implicit reader: Reads[T]): Future[T] = defaultProxy(body).map(_.as[T])

  def proxyD2[T](body: JsValue)(implicit reader: Reads[T]): Future[JsResult[T]] = defaultProxy(body).map(_.validate[T])

  /**
   * Sends `body` to the server and returns a [[Future]] of the `BODY` value of the response.
   *
   * @param body
   * @return
   */
  def defaultProxy(body: JsValue) = request(body, timeout)

  //  def proxyDinTermsOfValidate[T](body: JsValue)(implicit reader: Reads[T]): Future[JsResult[T]] =
  //    proxy(body).map(_.validate[T]).map(_.fold(???, identity))



  //  def completeLogged(requestID: String, response: JsValue) = {
  //    val ret = complete(requestID, response)
  //    ret.fold(log.warn(s"Invalid request ID: $requestID"))(isSuccess => {
  //      if (isSuccess) log info s"Completed request: $requestID with value: $response"
  //      else log info s"Unable to find channel with request ID: $requestID, cannot complete value: $response"
  //    })
  //    ret
  //  }
}

object JsonFutureSocket {
  def tryParseUUID(id: String): Option[UUID] = Utils.opt[UUID, IllegalArgumentException](UUID.fromString(id))
}
