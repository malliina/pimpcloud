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
import scala.util.Success

object NoCacheByteStreams {
  private val log = Logger(getClass)

  // backpressures automatically, seems to work fine, and does not consume RAM
  val ByteStringBufferSize = 0
}

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
    val uuid = UUID.randomUUID()
    val userAgent = req.headers.get(HeaderNames.USER_AGENT).map(ua => s"user agent $ua") getOrElse "unknown user agent"
    val describe = s"stream $uuid of track ${track.title} with range ${range.description} for $userAgent from ${req.remoteAddress}"
    val (queue, source) = Streaming.sourceQueue[ByteString](mat, NoCacheByteStreams.ByteStringBufferSize)
    iteratees += (uuid -> new ChannelInfo(queue, id, track, range))
    log.info(s"Created $describe")
    // Watches completion and disposes of resources early if the client (= mobile device) disconnects mid-request
    val src = source.watchTermination()((_, task) => task.onComplete(res => {
      val prefix = s"Completed $describe"
      res match {
        case Success(_) => log.info(prefix)
        case scala.util.Failure(t) => log.error(s"$prefix with failure", t)
      }
      remove(uuid, isCanceled = true)
    }))

    connectSource(uuid, src, track, range)
  }

  override def parser(uuid: UUID): Option[BodyParser[MultipartFormData[Long]]] = {
    get(uuid) map { info =>
      // info.send is called sequentially, i.e. the next send call occurs only after the previous call has completed
      StreamParsers.multiPartByteStreaming(bytes => info.send(bytes)
        .map(analyzeResult(info, bytes, _))
        .recoverWith(onOfferError(uuid, info, bytes)), maxUploadSize)(mat)
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

  def onOfferError(uuid: UUID, dest: StreamEndpoint, bytes: ByteString): PartialFunction[Throwable, Future[Unit]] = {
    case iae: IllegalArgumentException if Option(iae.getMessage).contains("Stream is terminated. SourceQueue is detached") =>
      log.info(s"Client disconnected $uuid")
      remove(uuid, isCanceled = true)
    case other: Throwable =>
      log.error(s"Offer of ${bytes.length} bytes failed for request $uuid", other)
      remove(uuid, isCanceled = true)
  }

  /** Transfer complete.
    *
    * TODO Since the transfer may have been cancelled prematurely by the recipient,
    * inform the server that it should stop uploading, to save network bandwidth.
    *
    * @param uuid the transfer ID
    */
  def disposeUUID(uuid: UUID): Future[Unit] = {
    (iteratees remove uuid)
      .map(_.close().map(_ => ()))
      .getOrElse(Future.successful(()))
  }

  def exists(uuid: UUID) = iteratees contains uuid

  def get(uuid: UUID): Option[StreamEndpoint] = iteratees get uuid
}
