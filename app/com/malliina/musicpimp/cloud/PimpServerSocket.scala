package com.malliina.musicpimp.cloud

import com.malliina.play.ContentRange
import com.malliina.ws.JsonFutureSocket
import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.audio.{Directory, Track}
import com.malliina.musicpimp.cloud.PimpMessages.Version
import com.malliina.musicpimp.cloud.PimpServerSocket.{body, idBody, nobody}
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.models._
import com.malliina.pimpcloud.ws.{CachedByteStreams, StreamBase}
import com.malliina.play.ws.SocketClient
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json._
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

/**
  * @author Michael
  */
class PimpServerSocket(channel: Channel[JsValue], id: String, val headers: RequestHeader, onUpdate: () => Unit)
  extends JsonFutureSocket(channel, id)
  with SocketClient[JsValue] {

  val fileTransfers: StreamBase[Array[Byte]] = new CachedByteStreams(id, channel, onUpdate)

  def streamRange(track: Track, contentRange: ContentRange): Option[Result] = {
    fileTransfers.streamRange(track, contentRange)
  }

  def ping = simpleProxy(PING)

  def pingAuth: Future[Version] = proxied[Version](nobody, VERSION)

  def meta(id: String): Future[Track] = proxyD[Track](nobody, META, idBody(id))

  /**
    * @param user username
    * @param pass password
    * @return true if authentication succeeds, false if the credentials are bogus or any failure occurs
    */
  def authenticate(user: String, pass: String): Future[Boolean] =
    authenticate3(user, pass).map(_ => true).recoverAll(_ => false)

  def authenticate3(user: String, pass: String): Future[Version] =
    proxied[Version](nobody, AUTHENTICATE, USERNAME -> user, PASSWORD -> pass)

  def rootFolder = proxied[Directory](nobody, ROOT_FOLDER)

  def folder(id: String) = proxyD[Directory](nobody, FOLDER, idBody(id))

  def search(term: String, limit: Int = PimpServerSocket.DefaultSearchLimit) = {
    proxied[Seq[Track]](nobody, SEARCH, TERM -> term, LIMIT -> limit)
  }

  def playlists(user: User) = proxied[PlaylistsMeta](nobody, PlaylistsGet, USERNAME -> user.name)

  def playlist(id: PlaylistID, user: User) = {
    proxied[PlaylistMeta](user, PlaylistGet, ID -> id.id, USERNAME -> user.name)
  }

  def deletePlaylist(id: PlaylistID, user: User) = {
    defaultProxy(user, PlaylistDelete, body(ID -> id.id, USERNAME -> user.name))
  }

  def alarms = simpleProxy(ALARMS)

  def status = simpleProxy(STATUS)

  protected def proxied[T](user: User, cmd: String, more: (String, Json.JsValueWrapper)*)(implicit reader: Reads[T]) = {
    proxyD[T](user, cmd, body(more: _*))
  }

  private def simpleProxy(cmd: String) = defaultProxy(nobody, cmd, Json.obj())
}

object PimpServerSocket {
  val DefaultSearchLimit = 100

  val nobody = User("nobody")

  def idBody(id: String): JsObject = body(ID -> id)

  /**
    * @return a JSON object with parameter `cmd` in key `cmd` and dictionary `more` in key `body`
    */
  def body(more: (String, Json.JsValueWrapper)*): JsObject = {
    Json.obj(more: _*)
  }
}
