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
import com.malliina.pimpcloud.models.{CloudID, FolderID, Identifiable, TrackID}
import com.malliina.pimpcloud.ws.{CachedByteStreams, NoCacheByteStreams, StreamBase}
import com.malliina.play.ContentRange
import com.malliina.play.models.{Password, Username}
import com.malliina.ws.JsonFutureSocket
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

object PimpServerSocket {
  val DefaultSearchLimit = 100
  val nobody = Username("nobody")

  def idBody(id: Identifiable): JsObject = body(Id -> id.id)

  /**
    * @return a JSON object with parameter `cmd` in key `cmd` and dictionary `more` in key `body`
    */
  def body(more: (String, Json.JsValueWrapper)*): JsObject =
    Json.obj(more: _*)
}

/**
  * @param channel
  * @param id the cloud ID of the server
  * @param headers
  * @param mat
  * @param onUpdate
  */
class PimpServerSocket(channel: SourceQueue[JsValue],
                       id: CloudID,
                       val headers: RequestHeader,
                       mat: Materializer,
                       onUpdate: () => Unit)
  extends JsonFutureSocket(channel, id) {

  val fileTransfers: StreamBase[ByteString] = new CachedByteStreams(id, channel, mat, onUpdate)

  def streamRange(track: Track, contentRange: ContentRange): Future[Option[Result]] =
    fileTransfers.streamRange(track, contentRange)

  def ping = simpleProxy(Ping)

  def pingAuth: Future[Version] = proxied[Version](nobody, VersionKey)

  def meta(id: TrackID): Future[Track] = proxyD[Track](nobody, Meta, idBody(id))

  /**
    * @param user username
    * @param pass password
    * @return true if authentication succeeds, false if the credentials are bogus or any failure occurs
    */
  def authenticate(user: Username, pass: Password): Future[Boolean] =
    authenticate3(user, pass).map(_ => true).recoverAll(_ => false)

  def authenticate3(user: Username, pass: Password): Future[Version] =
    proxied[Version](nobody, Authenticate, UsernameKey -> user, PasswordKey -> pass)

  def rootFolder = proxied[Directory](nobody, RootFolder)

  def folder(id: FolderID) = proxyD[Directory](nobody, FolderKey, idBody(id))

  def search(term: String, limit: Int = PimpServerSocket.DefaultSearchLimit) =
    proxied[Seq[Track]](nobody, SearchKey, Term -> term, Limit -> limit)

  def playlists(user: Username) = proxied[PlaylistsMeta](nobody, PlaylistsGet, UsernameKey -> user.name)

  def playlist(id: PlaylistID, user: Username) =
    proxied[PlaylistMeta](user, PlaylistGet, Id -> id.id, UsernameKey -> user.name)

  def deletePlaylist(id: PlaylistID, user: Username) =
    defaultProxy(user, PlaylistDelete, body(Id -> id.id, UsernameKey -> user.name))

  def alarms = simpleProxy(AlarmsKey)

  def status = simpleProxy(StatusKey)

  protected def proxied[T: Reads](user: Username, cmd: String, more: (String, Json.JsValueWrapper)*) =
    proxyD[T](user, cmd, body(more: _*))

  private def simpleProxy(cmd: String) = defaultProxy(nobody, cmd, Json.obj())
}
