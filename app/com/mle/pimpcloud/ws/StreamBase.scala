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
import play.api.mvc.{BodyParser, MultipartFormData}

import scala.util.{Failure, Success, Try}

/**
 * @author Michael
 */
trait StreamBase[T] extends Log {
  val maxUploadSize = 1024.megs

  def channel: Channel[JsValue]

  def snapshot: Seq[StreamData]

  //  def stream(track: Track): Option[Enumerator[T]] = stream(track, ContentRange.all(track.size))

  def stream(track: Track, range: ContentRange): Option[Enumerator[T]]

  def parser(uuid: UUID): Option[BodyParser[MultipartFormData[_]]]

  def exists(uuid: UUID): Boolean

  def withMessage(uuid: UUID, track: Track, range: ContentRange, onSuccess: => Enumerator[T]): Option[Enumerator[T]] = {
    streamChanged()
    val message =
      if (range.isAll) PimpSocket.fullTrackJson(track)
      else PimpSocket.rangedTrackJson(track, range)
    val payload = Json.obj(REQUEST_ID -> uuid) ++ message
    Try(channel push payload) match {
      case Success(()) =>
        log info s"Sent request: $uuid with body: $message"
        Some(onSuccess)
      case Failure(t) =>
        log.warn(s"Unable to send payload: $payload", t)
        remove(uuid)
        None
    }
  }

  def remove(uuid: UUID) = {
    removeUUID(uuid)
    streamChanged()
  }

  protected def removeUUID(uuid: UUID): Unit

  protected def streamChanged(): Unit = {
    Phones.updateRequestList()
  }
}