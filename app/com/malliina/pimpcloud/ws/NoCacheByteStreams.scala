package com.malliina.pimpcloud.ws

import java.util.UUID

import akka.stream.QueueOfferResult.{Dropped, Enqueued, Failure, QueueClosed}
import akka.stream.scaladsl.SourceQueue
import akka.stream.{Materializer, QueueOfferResult}
import akka.util.ByteString
import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.audio.Track
import com.malliina.pimpcloud.models.CloudID
import com.malliina.pimpcloud.streams.{ChannelInfo, StreamEndpoint}
import com.malliina.pimpcloud.ws.NoCacheByteStreams.log
import com.malliina.play.streams.StreamParsers
import com.malliina.play.{ContentRange, Streaming}
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.{BodyParser, MultipartFormData, RequestHeader, Result}
import play.mvc.Http.HeaderNames

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future

/** For each incoming request:
  *
  * 1) Assign an ID to the request
  * 2) Open a channel (or create a promise) onto which we push the eventual response
  * 3) Forward the request along with its ID to the destination server
  * 4) The destination server tags its response with the request ID
  * 5) Read the request ID from the response and push the response to the channel (or complete the promise)
  * 6) EOF and close the channel; this completes the request-response cycle
  */
class NoCacheByteStreams(id: CloudID,
                         val channel: SourceQueue[JsValue],
                         val mat: Materializer,
                         val onUpdate: () => Unit)
  extends ByteStreamBase {

  private val iteratees = TrieMap.empty[UUID, StreamEndpoint]

  def snapshot: Seq[StreamData] = iteratees.map {
    case (uuid, stream) => StreamData(uuid, id, stream.track, stream.range)
  }.toSeq

  /**
    * @return a Result if the server received the upload request, None otherwise
    * @see https://groups.google.com/forum/#!searchin/akka-user/source.queue/akka-user/zzGSuRG4YVA/NEjwAT76CAAJ
    */
  def requestTrack(track: Track, range: ContentRange, req: RequestHeader): Future[Option[Result]] = {
    val (queue, source) = Streaming.sourceQueue[ByteString](mat)
    val uuid = UUID.randomUUID()
    val userAgent = req.headers.get(HeaderNames.USER_AGENT) getOrElse "undefined"
    log.info(s"Created request $uuid of track ${track.title} with range ${range.description} for user agent $userAgent")
    iteratees += (uuid -> new ChannelInfo(queue, id, track, range))
    connectSource(uuid, source, track, range)
  }

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

  /** Transfer complete.
    *
    * @param uuid
    */
  def removeUUID(uuid: UUID): Future[Unit] = {
    (iteratees remove uuid)
      .map(_.close().map(_ => ()))
      .getOrElse(Future.successful(()))
  }

  def exists(uuid: UUID) = iteratees contains uuid

  def get(uuid: UUID): Option[StreamEndpoint] = iteratees get uuid

  //  def requestTrack(track: Track, range: ContentRange): Future[Option[Result]] = {
  //    val (actor, source) = Streaming.actorSource(mat)
  //    val uuid = UUID.randomUUID()
  //    iteratees += (uuid -> new ActorStream(actor, track, range))
  //    connectSource(uuid, source, track, range)
  //  }
}

object NoCacheByteStreams {
  private val log = Logger(getClass)
}
