package com.malliina.pimpcloud.ws

import java.util.UUID

import akka.stream.scaladsl.{Source, SourceQueue}
import akka.stream.{Materializer, QueueOfferResult}
import akka.util.ByteString
import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.cloud.{PimpServerSocket, UserRequest}
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.pimpcloud.ws.StreamBase.log
import com.malliina.play.ContentRange
import com.malliina.ws.Streamer
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

object StreamBase {
  private val log = Logger(getClass)
}

trait StreamBase[T] extends Streamer {
  def mat: Materializer

  implicit def ec = mat.executionContext

  def onUpdate: () => Unit

  def channel: SourceQueue[JsValue]

  def snapshot: Seq[StreamData]

  def requestTrack(track: Track, range: ContentRange, req: RequestHeader): Future[Option[Result]]

  def exists(uuid: UUID): Boolean

  protected def disposeUUID(uuid: UUID): Future[Unit]

  override def remove(uuid: UUID, isCanceled: Boolean): Future[Unit] = {
    val disposal = disposeUUID(uuid)
    val cancellation = if (isCanceled) sendMessage(cancelMessage(uuid)) else Future.successful(())
    val op = for {
      d <- disposal
      c <- cancellation
    } yield {
      ()
    }
    op.recoverAll(t => log.error(s"Disposal failed for request $uuid", t)) map { _ =>
      log info s"Notifying listeners of changed streams due to removal of $uuid"
      streamChanged()
    }
  }

  protected def connectSource(uuid: UUID, source: Source[ByteString, _], track: Track, range: ContentRange): Future[Option[Result]] = {
    val result = resultify(source, range)
    val connectSuccess = connect(uuid, track, range)
    connectSuccess.map(isSuccess => if (isSuccess) Option(result) else None)
  }

  protected def resultify(source: Source[ByteString, _], range: ContentRange): Result

  /**
    * @return true if the server received the upload request, false otherwise
    */
  private def connect(uuid: UUID, track: Track, range: ContentRange): Future[Boolean] = {
    tryConnect(uuid, track, range) map { result =>
      log debug s"Connected $uuid for ${track.title} with range $range"
      true
    } recover {
      case t =>
        log.warn(s"Unable to connect $uuid for ${track.title} with range $range", t)
        remove(uuid, isCanceled = true)
        false
    }
  }

  private def tryConnect(uuid: UUID, track: Track, range: ContentRange): Future[QueueOfferResult] = {
    streamChanged()
    def trackRequest(body: JsValue) = UserRequest(TrackKey, body, uuid, PimpServerSocket.nobody)
    val body =
      if (range.isAll) PimpServerSocket.idBody(track.id)
      else PimpServerSocket.body(Id -> track.id, Range -> range)
    sendMessage(trackRequest(body))
  }

  def sendMessage[M: Writes](msg: M) = channel offer Json.toJson(msg)

  def cancelMessage(uuid: UUID) = UserRequest(Cancel, Json.obj(), uuid, PimpServerSocket.nobody)

  protected def streamChanged(): Unit = onUpdate()
}
