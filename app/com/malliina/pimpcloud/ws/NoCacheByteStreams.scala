package com.malliina.pimpcloud.ws

import java.util.UUID

import akka.stream.QueueOfferResult.{Dropped, Enqueued, Failure, QueueClosed}
import akka.stream.scaladsl.SourceQueue
import akka.stream.{Materializer, QueueOfferResult}
import akka.util.ByteString
import com.malliina.concurrent.FutureOps
import com.malliina.pimpcloud.models.CloudID
import com.malliina.pimpcloud.streams.StreamEndpoint
import com.malliina.pimpcloud.ws.NoCacheByteStreams.log
import com.malliina.play.streams.StreamParsers
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.{BodyParser, MultipartFormData}

import scala.concurrent.Future

class NoCacheByteStreams(id: CloudID,
                         channel: SourceQueue[JsValue],
                         val mat: Materializer,
                         val onUpdate: () => Unit)
  extends CloudStreams[ByteString](id, channel, mat)
    with ByteStreamBase {

  override def parser(uuid: UUID): Option[BodyParser[MultipartFormData[Long]]] = {
    get(uuid) map { info =>
      StreamParsers.multiPartByteStreaming(bytes => info.send(bytes)
        .map(analyzeResult(info, bytes, _))
        .recoverAll(onOfferError(uuid, info, bytes, _)), maxUploadSize)(mat)
    }
  }

  def analyzeResult(dest: StreamEndpoint, bytes: ByteString, result: QueueOfferResult) = {
    val suffix = s" for ${bytes.length} bytes after offers"
    result match {
      case Enqueued => ()
      case Dropped => log.warn(s"Offer dropped$suffix")
      case Failure(t) => log.error(s"Offer failed$suffix", t)
      case QueueClosed => () //log.error(s"Queue closed$suffix")
    }
  }

  def onOfferError(uuid: UUID, dest: StreamEndpoint, bytes: ByteString, t: Throwable): PartialFunction[Throwable, Future[Unit]] = {
    case iae: IllegalArgumentException if Option(iae.getMessage).contains("Stream is terminated. SourceQueue is detached") =>
      log.info(s"Client disconnected $uuid")
      remove(uuid)
    case other: Throwable =>
      log.error(s"Offer of ${bytes.length} bytes failed for request $uuid", t)
      remove(uuid)
  }
}

object NoCacheByteStreams {
  private val log = Logger(getClass)
}
