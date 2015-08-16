package com.mle.pimpcloud.ws

import java.util.UUID

import com.mle.musicpimp.audio.Track
import com.mle.play.concurrent.ExecutionContexts.synchronousIO
import com.mle.play.streams.StreamParsers
import com.mle.play.{ContentRange, Enumerators}
import com.mle.storage.StorageInt
import com.mle.util.Log
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.JsValue
import play.api.mvc.{BodyParser, MultipartFormData}
import rx.lang.scala.Observable
import rx.lang.scala.subjects.ReplaySubject

import scala.collection.concurrent.TrieMap

/**
 * We might receive multiple requests of the same resource (song) within a short period of time. We do not want to
 * strain the music source which might be a bottleneck, so we cache streams if they're not greater than
 * `cacheThreshold`. Caching consumes memory, but this trade-off is accepted. The streams are cached for as long as the
 * first stream is ongoing: client code must call `remove(UUID)` once the stream is complete.
 *
 * Falls back to non-cached streaming if the track is meets or exceeds `cacheThreshold`.
 *
 * @author Michael
 */
class CachedByteStreams(id: String, val channel: Channel[JsValue])
  extends StreamBase[Array[Byte]] with Log {
  val cacheThreshold = 19.megs
  private val cachedStreams = TrieMap.empty[UUID, StreamInfo]
  private val notCached = new NoCacheCloudStreams(id, channel)

  override def snapshot = cachedStreams.map(kv => StreamData(kv._1, id, kv._2.track, kv._2.range)).toSeq ++ notCached.snapshot

  override def stream(track: Track, range: ContentRange): Option[Enumerator[Array[Byte]]] =
    attachToOngoing(track, range) orElse send(track, range)

  private def attachToOngoing(track: Track, range: ContentRange): Option[Enumerator[Array[Byte]]] = {
    val title = track.title
    cachedStreams.values.find(s => s.track == track && s.range == range).map(info => {
      log info s"Attaching to ongoing stream of $title, range $range"
      enumerator(info.stream)
    })
  }

  private def send(track: Track, range: ContentRange): Option[Enumerator[Array[Byte]]] = {
    val title = track.title
    val trackSize = track.size
    if (trackSize > cacheThreshold) {
      log info s"Non-cached streaming of $title, as its size $trackSize exceeds the maximum of $cacheThreshold, range $range"
      notCached.stream(track, range)
    } else {
      log info s"Cached streaming of $title, range $range"
      val subject = ReplaySubject[Array[Byte]]()
      val uuid = UUID.randomUUID()
      cachedStreams += (uuid -> StreamInfo(track, range, subject))
      withMessage(uuid, track, range, enumerator(subject))
    }
  }

  def enumerator[T](obs: Observable[T]) = Enumerators.fromObservableOneShot(obs)

  override def exists(uuid: UUID): Boolean = (cachedStreams contains uuid) || (notCached exists uuid)

  override def parser(uuid: UUID): Option[BodyParser[MultipartFormData[_]]] =
    cachedStreams.get(uuid)
      .map(info => StreamParsers.multiPartByteStreaming(bytes => info.stream.onNext(bytes), maxUploadSize))
      .orElse(notCached parser uuid)

  def removeUUID(uuid: UUID): Unit = {
    (cachedStreams remove uuid).foreach(si => si.stream.onCompleted())
    notCached remove uuid
  }
}
