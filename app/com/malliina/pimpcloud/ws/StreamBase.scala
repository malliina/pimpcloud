package com.malliina.pimpcloud.ws

import java.util.UUID

import akka.NotUsed
import akka.stream.{Materializer, QueueOfferResult}
import akka.stream.scaladsl.{Source, SourceQueue}
import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.cloud.{PimpServerSocket, UserRequest}
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.pimpcloud.ws.StreamBase.log
import com.malliina.play.ContentRange
import com.malliina.storage.StorageInt
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{BodyParser, MultipartFormData, Result}

import scala.concurrent.Future

trait StreamBase[T] {
  val maxUploadSize = 1024.megs

  def mat: Materializer

  implicit def ec = mat.executionContext

  def onUpdate: () => Unit

  def channel: SourceQueue[JsValue]

  def snapshot: Seq[StreamData]

  def streamRange(track: Track, range: ContentRange): Future[Option[Result]]

  def parser(uuid: UUID): Option[BodyParser[MultipartFormData[_]]]

  def exists(uuid: UUID): Boolean

  def remove(uuid: UUID) = {
    removeUUID(uuid)
    streamChanged()
  }

  protected def connectSource(uuid: UUID, source: Source[T, NotUsed], track: Track, range: ContentRange): Future[Option[Result]] = {
    val result = resultify(source, range)
    val connectSuccess = connect(uuid, track, range)
    connectSuccess.map(isSuccess => if(isSuccess) Option(result) else None)
  }

  protected def resultify(source: Source[T, NotUsed], range: ContentRange): Result

  private def connect(uuid: UUID, track: Track, range: ContentRange): Future[Boolean] = {
    tryConnect(uuid, track, range) map { result =>
      log info s"Connected $uuid for ${track.title} with range $range"
      true
    } recover {
      case t =>
        log.warn(s"Unable to connect $uuid for ${track.title} with range $range", t)
        remove(uuid)
        false
    }
  }

  private def tryConnect(uuid: UUID, track: Track, range: ContentRange): Future[QueueOfferResult] = {
    streamChanged()
    val message =
      if (range.isAll) UserRequest(TRACK, PimpServerSocket.idBody(track.id), uuid, PimpServerSocket.nobody)
      else UserRequest(TRACK, PimpServerSocket.body(ID -> track.id, RANGE -> range), uuid, PimpServerSocket.nobody)
    val payload = Json.toJson(message)
    channel offer payload
  }

  protected def removeUUID(uuid: UUID): Unit

  protected def streamChanged(): Unit = onUpdate()
}

object StreamBase {
  private val log = Logger(getClass)
}
