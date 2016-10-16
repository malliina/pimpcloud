package com.malliina.pimpcloud.ws

import java.util.UUID

import akka.stream.Materializer
import akka.stream.scaladsl.SourceQueue
import akka.util.ByteString
import com.malliina.musicpimp.audio.Track
import com.malliina.pimpcloud.models.CloudID
import com.malliina.pimpcloud.streams.{ChannelInfo, StreamEndpoint}
import com.malliina.pimpcloud.ws.CloudStreams.log
import com.malliina.play.{ContentRange, Streaming}
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.{RequestHeader, Result}
import play.mvc.Http.HeaderNames

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future

object CloudStreams {
  private val log = Logger(getClass)
}

/** For each incoming request:
  *
  * 1) Assign an ID to the request
  * 2) Open a channel (or create a promise) onto which we push the eventual response
  * 3) Forward the request along with its ID to the destination server
  * 4) The destination server tags its response with the request ID
  * 5) Read the request ID from the response and push the response to the channel (or complete the promise)
  * 6) EOF and close the channel; this completes the request-response cycle
  *
  * @tparam T type of stream element, e.g. a ByteString
  */
abstract class CloudStreams[T](id: CloudID, val channel: SourceQueue[JsValue], mat: Materializer)
  extends StreamBase[T] {

  private val iteratees = TrieMap.empty[UUID, StreamEndpoint]

  def snapshot: Seq[StreamData] = iteratees.map(kv => StreamData(kv._1, id, kv._2.track, kv._2.range)).toSeq

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

  //  def requestTrack(track: Track, range: ContentRange): Future[Option[Result]] = {
  //    val (actor, source) = Streaming.actorSource(mat)
  //    val uuid = UUID.randomUUID()
  //    iteratees += (uuid -> new ActorStream(actor, track, range))
  //    connectSource(uuid, source, track, range)
  //  }

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
}
