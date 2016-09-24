package com.malliina.musicpimp.cloud

import akka.stream.Materializer
import akka.stream.scaladsl.SourceQueue
import akka.util.ByteString
import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.audio.{Directory, Track}
import com.malliina.musicpimp.cloud.PimpMessages.Version
import com.malliina.musicpimp.cloud.PimpServerSocket.{body, idBody, nobody}
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.models._
import com.malliina.pimpcloud.ws.{NoCacheByteStreams, StreamBase}
import com.malliina.play.ContentRange
import com.malliina.play.models.Username
import com.malliina.ws.JsonFutureSocket
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

class PimpServerSocket(channel: SourceQueue[JsValue],
                       id: Username,
                       val headers: RequestHeader,
                       mat: Materializer,
                       onUpdate: () => Unit)
  extends JsonFutureSocket(channel, id) {

  val fileTransfers: StreamBase[ByteString] = new NoCacheByteStreams(id, channel, mat, onUpdate)

  def streamRange(track: Track, contentRange: ContentRange): Future[Option[Result]] =
    fileTransfers.streamRange(track, contentRange)

  def ping = simpleProxy(PING)

  def pingAuth: Future[Version] = proxied[Version](nobody, VERSION)

  def meta(id: String): Future[Track] = proxyD[Track](nobody, META, idBody(id))

  /**
    * @param user username
    * @param pass password
    * @return true if authentication succeeds, false if the credentials are bogus or any failure occurs
    */
  def authenticate(user: Username, pass: String): Future[Boolean] =
    authenticate3(user, pass).map(_ => true).recoverAll(_ => false)

  def authenticate3(user: Username, pass: String): Future[Version] =
    proxied[Version](nobody, AUTHENTICATE, USERNAME -> user, PASSWORD -> pass)

  def rootFolder = proxied[Directory](nobody, ROOT_FOLDER)

  def folder(id: String) = proxyD[Directory](nobody, FOLDER, idBody(id))

  def search(term: String, limit: Int = PimpServerSocket.DefaultSearchLimit) =
    proxied[Seq[Track]](nobody, SEARCH, TERM -> term, LIMIT -> limit)

  def playlists(user: Username) = proxied[PlaylistsMeta](nobody, PlaylistsGet, USERNAME -> user.name)

  def playlist(id: PlaylistID, user: Username) =
    proxied[PlaylistMeta](user, PlaylistGet, ID -> id.id, USERNAME -> user.name)

  def deletePlaylist(id: PlaylistID, user: Username) =
    defaultProxy(user, PlaylistDelete, body(ID -> id.id, USERNAME -> user.name))

  def alarms = simpleProxy(ALARMS)

  def status = simpleProxy(STATUS)

  protected def proxied[T: Reads](user: Username, cmd: String, more: (String, Json.JsValueWrapper)*) =
    proxyD[T](user, cmd, body(more: _*))

  private def simpleProxy(cmd: String) = defaultProxy(nobody, cmd, Json.obj())
}

object PimpServerSocket {
  val DefaultSearchLimit = 100

  val nobody = Username("nobody")

  def idBody(id: String): JsObject = body(ID -> id)

  /**
    * @return a JSON object with parameter `cmd` in key `cmd` and dictionary `more` in key `body`
    */
  def body(more: (String, Json.JsValueWrapper)*): JsObject =
    Json.obj(more: _*)
}
