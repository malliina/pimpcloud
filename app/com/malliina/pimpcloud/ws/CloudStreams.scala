package com.malliina.pimpcloud.ws

import java.util.UUID

import akka.stream.Materializer
import akka.stream.scaladsl.SourceQueue
import com.malliina.musicpimp.audio.Track
import com.malliina.play.models.Username
import com.malliina.play.{ContentRange, Streaming}
import play.api.libs.json.JsValue
import play.api.mvc.Result

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
  *
  * @tparam T type of stream element, e.g. a ByteString
  */
abstract class CloudStreams[T](id: Username, val channel: SourceQueue[JsValue], mat: Materializer)
  extends StreamBase[T] {
  private val iteratees = TrieMap.empty[UUID, ChannelInfo[T]]

  def snapshot: Seq[StreamData] = iteratees.map(kv => StreamData(kv._1, id, kv._2.track, kv._2.range)).toSeq

  /**
    * @return a Result if the server received the upload request, None otherwise
    * @see https://groups.google.com/forum/#!searchin/akka-user/source.queue/akka-user/zzGSuRG4YVA/NEjwAT76CAAJ
    */
  def streamRange(track: Track, range: ContentRange): Future[Option[Result]] = {
    val (queue, source) = Streaming.sourceQueue[T](mat)
    val uuid = UUID.randomUUID()
    iteratees += (uuid -> ChannelInfo(queue, id, track, range))
    connectSource(uuid, source, track, range)
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

  def get(uuid: UUID): Option[ChannelInfo[T]] = iteratees get uuid
}

case class ChannelInfo[T](channel: SourceQueue[Option[T]],
                          serverID: Username,
                          track: Track,
                          range: ContentRange) {
  def send(t: T) = channel.offer(Option(t))

  def close() = channel.offer(None)
}
