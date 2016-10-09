package com.malliina.ws

import java.util.UUID

import akka.stream.scaladsl.SourceQueue
import com.malliina.musicpimp.cloud.UuidFutureMessaging
import com.malliina.musicpimp.json.JsonStrings.{BODY, REQUEST_ID, SUCCESS}
import com.malliina.pimpcloud.models.CloudID
import com.malliina.play.models.Username
import com.malliina.util.Utils
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

/** Emulates an HTTP client using a WebSocket channel. Supports timeouts.
  *
  * Protocol: Responses must be tagged with the same request ID we add to sent messages, so that we can
  * pair requests with responses.
  */
class JsonFutureSocket(val channel: SourceQueue[JsValue], val id: CloudID)
  extends UuidFutureMessaging
    with com.malliina.play.ws.SocketClient[JsValue] {

  val timeout = 20.seconds

  /** We expect responses to contain `REQUEST_ID` and `BODY` keys, which we parse. The request is completed with the
    * JSON object in `BODY`.
    *
    * @param response a JSON response from server
    * @return a response, parsed
    */
  override def extract(response: JsValue): Option[BodyAndId] =
    for {
      uuid <- (response \ REQUEST_ID).asOpt[UUID]
      body <- (response \ BODY).toOption
    } yield BodyAndId(body, uuid)

  override def isSuccess(response: JsValue): Boolean =
    (response \ SUCCESS).validate[Boolean].filter(_ == false).isError

  //  /** TODO Fix signature; what happens when the channel is closed and this method is called?
  //    *
  //    * @param json payload
  //    */
  //  def send(json: JsValue): Future[QueueOfferResult] = channel offer json

  /** Sends `body` as JSON and deserializes the response to `U`.
    *
    * @param body message payload
    * @tparam T type of request payload
    * @tparam U type of response
    * @return the response
    */
  def proxyT[T: Writes, U: Reads](cmd: String, body: T, user: Username): Future[U] =
    proxyD(user, cmd, Json.toJson(body))

  /** Sends `body` and deserializes the response to type `T`.
    *
    * TODO check success status first, and any potential error
    *
    * @param body payload
    * @tparam T type of response
    * @return a deserialized body, or a failed [[Future]] on failure
    */
  def proxyD[T: Reads](user: Username, cmd: String, body: JsValue): Future[T] =
    proxyD2[T](user, cmd, body).map(_.get)

  def proxyD2[T: Reads](user: Username, cmd: String, body: JsValue): Future[JsResult[T]] =
    defaultProxy(user, cmd, body).map(_.validate[T])

  /**
    * @param body payload
    * @return response
    */
  def defaultProxy(user: Username, cmd: String, body: JsValue): Future[JsValue] =
    request(cmd, body, user, timeout)
}

object JsonFutureSocket {
  def tryParseUUID(id: String): Option[UUID] = {
    Utils.opt[UUID, IllegalArgumentException](UUID.fromString(id))
  }
}
