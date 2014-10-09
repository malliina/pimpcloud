package com.mle.musicpimp.cloud

import com.mle.concurrent.FutureImplicits.RichFuture
import com.mle.musicpimp.audio.{Directory, Track}
import com.mle.musicpimp.cloud.PimpMessages.Version
import com.mle.musicpimp.cloud.PimpSocket.{json, jsonID}
import com.mle.musicpimp.json.JsonStrings._
import com.mle.play.ws.SocketClient
import com.mle.ws.JsonFutureSocket
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.RequestHeader

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * @author Michael
 */
class PimpSocket(channel: Channel[JsValue], id: String, val headers: RequestHeader)
  extends JsonFutureSocket(channel, id)
  with SocketClient[JsValue] {

  def ping = simpleProxy(PING)

  def pingAuth: Future[Version] = proxyD[Version](json(VERSION))

  def meta(id: String): Future[Track] = proxyD[Track](jsonID(META, id))

  /**
   *
   * @param user username
   * @param pass password
   * @return true if authentication succeeds, false if the credentials are bogus or any failure occurs
   */
  def authenticate(user: String, pass: String): Future[Boolean] =
    authenticate3(user, pass).map(_ => true).recoverAll(_ => false)

  def authenticate3(user: String, pass: String): Future[Version] =
    proxyD[Version](json(AUTHENTICATE, USERNAME -> user, PASSWORD -> pass))

  def rootFolder = proxyD[Directory](json(ROOT_FOLDER))

  def folder(id: String) = proxyD[Directory](jsonID(FOLDER, id))

  def search(term: String, limit: Int = 100) = proxyD[Seq[Track]](json(SEARCH, TERM -> term, LIMIT -> limit))

  def alarms = simpleProxy(ALARMS)

  def status = simpleProxy(STATUS)

  private def simpleProxy(cmd: String) = defaultProxy(json(cmd))
}

object PimpSocket {
  def jsonID(cmd: String, id: String): JsObject = json(cmd, ID -> id)

  def json(cmd: String, more: (String, Json.JsValueWrapper)*): JsObject = bodiedJson(cmd, Json.obj(more: _*))

  def bodiedJson(cmd: String, more: JsObject): JsObject = Json.obj(CMD -> cmd) ++ more
}
