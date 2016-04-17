package com.malliina.pimpcloud.ws

import java.util.UUID

import akka.stream.Materializer
import akka.stream.scaladsl.SourceQueue
import com.malliina.musicpimp.audio.Track
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
abstract class CloudStreams[T](id: String, val channel: SourceQueue[JsValue], mat: Materializer)
  extends StreamBase[T] {
  private val iteratees = TrieMap.empty[UUID, IterateeInfo[T]]

  def snapshot: Seq[StreamData] = iteratees.map(kv => StreamData(kv._1, id, kv._2.track, kv._2.range)).toSeq

  /**
    *
    * @param track
    * @param range
    * @return
    * @see https://groups.google.com/forum/#!searchin/akka-user/source.queue/akka-user/zzGSuRG4YVA/NEjwAT76CAAJ
    */
  def streamRange(track: Track, range: ContentRange): Future[Option[Result]] = {
    val (queue, source) = Streaming.sourceQueue[T](mat)
    val uuid = UUID.randomUUID()
    iteratees += (uuid -> IterateeInfo(queue, id, track, range))
    connectEnumerator(uuid, source, track, range)
  }

  /** Transfer complete.
    *
    * @param uuid
    */
  def removeUUID(uuid: UUID) = (iteratees remove uuid).foreach(_.close())

  def exists(uuid: UUID) = iteratees contains uuid

  def get(uuid: UUID) = iteratees get uuid
}

case class IterateeInfo[T](channel: SourceQueue[Option[T]],
                           serverID: String,
                           track: Track,
                           range: ContentRange) {
  def send(t: T) = channel.offer(Option(t))

  def close() = channel.offer(None)
}
