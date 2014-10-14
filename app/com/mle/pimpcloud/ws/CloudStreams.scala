package com.mle.pimpcloud.ws

import java.util.UUID

import com.mle.musicpimp.audio.Track
import com.mle.musicpimp.cloud.PimpSocket
import com.mle.musicpimp.json.JsonStrings.{BODY, REQUEST_ID, TRACK}
import com.mle.util.Log
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.libs.json.{JsValue, Json}

import scala.collection.concurrent.TrieMap
import scala.util.{Failure, Success, Try}

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
abstract class CloudStreams[T](id: String, channel: Channel[JsValue]) extends StreamBase[T] with Log {
  private val iteratees = TrieMap.empty[UUID, IterateeInfo[T]]

  def snapshot: Seq[StreamData] = iteratees.map(kv => StreamData(kv._1, id, kv._2.track)).toSeq

  def stream(track: Track): Option[Enumerator[T]] = {
    val message = PimpSocket.jsonID(TRACK, track.id)
    val (iteratee, enumerator) = Concurrent.joined[T]
    val uuid = UUID.randomUUID()
    iteratees += (uuid -> IterateeInfo(iteratee, id, track))
    streamChanged()
    val payload = Json.obj(REQUEST_ID -> uuid.toString, BODY -> message)
    val ret = Try(channel push payload)
    ret match {
      case Success(()) =>
        log debug s"Sent request: $uuid with body: $message"
        Some(enumerator)
      case Failure(t) =>
        log.warn(s"Unable to send payload: $payload", t)
        remove(uuid)
        None
    }
  }

  def removeUUID(uuid: UUID) = {
    iteratees remove uuid
  }

  def exists(uuid: UUID) = iteratees contains uuid

  def get(uuid: UUID): Option[Iteratee[T, Unit]] = iteratees get uuid map (_.iteratee)
}

case class IterateeInfo[T](iteratee: Iteratee[T, Unit], serverID: String, track: Track)
