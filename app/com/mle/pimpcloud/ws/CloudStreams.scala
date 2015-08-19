package com.mle.pimpcloud.ws

import java.util.UUID

import com.mle.musicpimp.audio.Track
import com.mle.play.ContentRange
import com.mle.util.Log
import play.api.libs.iteratee.Concurrent
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.JsValue
import play.api.mvc.Result

import scala.collection.concurrent.TrieMap

/**
 * For each incoming request:
 *
 * 1) Assign an ID to the request
 * 2) Open a channel (or create a promise) onto which we push the eventual response
 * 3) Forward the request along with its ID to the destination server
 * 4) The destination server tags its response with the request ID
 * 5) Read the request ID from the response and push the response to the channel (or complete the promise)
 * 6) EOF and close the channel; this completes the request-response cycle
 */
abstract class CloudStreams[T](id: String, val channel: Channel[JsValue]) extends StreamBase[T] with Log {
  private val iteratees = TrieMap.empty[UUID, IterateeInfo[T]]

  def snapshot: Seq[StreamData] = iteratees.map(kv => StreamData(kv._1, id, kv._2.track, kv._2.range)).toSeq

  def streamRange(track: Track, range: ContentRange): Option[Result] = {
    val (enumerator, channel) = Concurrent.broadcast[T]
    val uuid = UUID.randomUUID()
    iteratees += (uuid -> IterateeInfo(channel, id, track, range))
    connectEnumerator(uuid, enumerator, track, range)
  }

  /**
   * Transfer complete.
   *
   * @param uuid
   */
  def removeUUID(uuid: UUID) = {
    (iteratees remove uuid).foreach(ii => ii.channel.eofAndEnd())
  }

  def exists(uuid: UUID) = iteratees contains uuid

  def get(uuid: UUID) = iteratees get uuid
}

case class IterateeInfo[T](channel: Concurrent.Channel[T],
                           serverID: String,
                           track: Track,
                           range: ContentRange)
