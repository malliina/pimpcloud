package com.mle.pimpcloud.ws

import java.util.UUID

import com.mle.musicpimp.audio.Track
import com.mle.musicpimp.cloud.PimpSocket
import com.mle.musicpimp.json.JsonStrings._
import com.mle.play.ContentRange
import com.mle.storage.StorageInt
import com.mle.util.Log
import controllers.Phones
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{BodyParser, MultipartFormData, Result}

import scala.util.{Failure, Success, Try}

/**
 * @author Michael
 */
trait StreamBase[T] extends Log {
  val maxUploadSize = 1024.megs

  def channel: Channel[JsValue]

  def snapshot: Seq[StreamData]

  def streamRange(track: Track, range: ContentRange): Option[Result]

  def parser(uuid: UUID): Option[BodyParser[MultipartFormData[_]]]

  def exists(uuid: UUID): Boolean

  def remove(uuid: UUID) = {
    removeUUID(uuid)
    streamChanged()
  }

  protected def connectEnumerator(uuid: UUID, enumerator: Enumerator[T], track: Track, range: ContentRange): Option[Result] = {
    val result = resultify(enumerator, range)
    val connectSuccess = connect(uuid, track, range)
    if (connectSuccess) Option(result)
    else None
  }

  protected def resultify(enumerator: Enumerator[T], range: ContentRange): Result

  private def connect(uuid: UUID, track: Track, range: ContentRange) = {
    tryConnect(uuid, track, range) match {
      case Success(()) =>
        log info s"Connected $uuid for ${track.title} with range $range"
        true
      case Failure(t) =>
        log.warn(s"Unable to connect $uuid for ${track.title} with range $range", t)
        remove(uuid)
        false
    }
  }

  private def tryConnect(uuid: UUID, track: Track, range: ContentRange): Try[Unit] = {
    streamChanged()
    val message =
      if (range.isAll) PimpSocket.fullTrackJson(track)
      else PimpSocket.rangedTrackJson(track, range)
    val payload = Json.obj(REQUEST_ID -> uuid) ++ message
    Try(channel push payload)
  }

  protected def removeUUID(uuid: UUID): Unit

  protected def streamChanged(): Unit = {
    Phones.updateRequestList()
  }
}
