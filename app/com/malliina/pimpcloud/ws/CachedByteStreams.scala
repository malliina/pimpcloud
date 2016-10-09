package com.malliina.pimpcloud.ws

import java.util.UUID

import akka.stream.Materializer
import akka.stream.scaladsl.SourceQueue
import akka.util.ByteString
import com.malliina.musicpimp.audio.Track
import com.malliina.pimpcloud.models.CloudID
import com.malliina.pimpcloud.ws.CachedByteStreams.log
import com.malliina.play.models.Username
import com.malliina.play.streams.StreamParsers
import com.malliina.play.{ContentRange, StreamConversions}
import com.malliina.storage.StorageInt
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.{BodyParser, MultipartFormData, Result}
import rx.lang.scala.Observable
import rx.lang.scala.subjects.ReplaySubject

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future

/** We might receive multiple requests of the same resource (song) within a short period of time. We do not want to
  * strain the music source which might be a bottleneck, so we cache streams if they're not greater than
  * `cacheThreshold`. Caching consumes memory, but this trade-off is accepted. The streams are cached for as long as the
  * first stream is ongoing: client code must call `remove(UUID)` once the stream is complete.
  *
  * Falls back to non-cached streaming if the track is meets or exceeds `cacheThreshold`.
  */
class CachedByteStreams(id: CloudID,
                        val channel: SourceQueue[JsValue],
                        val mat: Materializer,
                        val onUpdate: () => Unit)
  extends ByteStreamBase {

  val cacheThreshold = 256.megs
  private val cachedStreams = TrieMap.empty[UUID, StreamInfo]
  private val notCached = new NoCacheByteStreams(id, channel, mat, onUpdate)

  override def snapshot = cachedStreams.map(kv => StreamData(kv._1, id, kv._2.track, kv._2.range)).toSeq ++ notCached.snapshot

  override def exists(uuid: UUID): Boolean = (cachedStreams contains uuid) || (notCached exists uuid)

  override def parser(uuid: UUID): Option[BodyParser[MultipartFormData[_]]] =
    cachedStreams.get(uuid)
      .map(info => StreamParsers.multiPartByteStreaming(bytes => Future.successful(info.stream.onNext(bytes)), maxUploadSize)(mat))
      .orElse(notCached parser uuid)

  override def streamRange(track: Track, range: ContentRange): Future[Option[Result]] =
    attachToOngoing(track, range).map(r => Future.successful(Option(r))) getOrElse send(track, range)

  private def attachToOngoing(track: Track, range: ContentRange): Option[Result] = {
    val title = track.title
    cachedStreams.values.find(s => s.track == track && s.range == range) map { info =>
      log info s"Attaching to ongoing stream of $title, range $range"
      resultify(toSource(info.stream), range)
    }
  }

  private def send(track: Track, range: ContentRange): Future[Option[Result]] = {
    val title = track.title
    val trackSize = track.size
    if (trackSize > cacheThreshold) {
      log info s"Non-cached streaming of $title, as its size $trackSize exceeds the maximum of $cacheThreshold, range $range"
      notCached.streamRange(track, range)
    } else {
      log info s"Cached streaming of $title, range $range"
      val subject = ReplaySubject[ByteString]()
      val uuid = UUID.randomUUID()
      cachedStreams += (uuid -> StreamInfo(track, range, subject))
      connectSource(uuid, toSource(subject), track, range)
    }
  }

  def toSource[T](obs: Observable[T]) = StreamConversions.observableToSource(obs, mat)

  def removeUUID(uuid: UUID): Future[Unit] = {
    (cachedStreams remove uuid).foreach(si => si.stream.onCompleted())
    notCached remove uuid
  }
}

object CachedByteStreams {
  private val log = Logger(getClass)
}
