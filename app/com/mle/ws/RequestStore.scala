package com.mle.ws

import java.util.UUID

import com.mle.musicpimp.json.JsonStrings._
import com.mle.util.Log
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee.{Concurrent, Enumerator}
import play.api.libs.json.{JsValue, Json}

import scala.collection.concurrent.TrieMap
import scala.util.{Failure, Success, Try}

/**
 * @author Michael
 */
class RequestStore[T] extends Log {
  val ongoingRequests = TrieMap.empty[UUID, Channel[T]]

  def send(message: JsValue, channel: Channel[JsValue]): Option[Enumerator[T]] = {
    val (enumerator, responseChannel) = Concurrent.broadcast[T]
    val uuid = UUID.randomUUID()
    ongoingRequests += (uuid -> responseChannel)
    val payload = Json.obj(REQUEST_ID -> uuid.toString, BODY -> message)
    val ret = Try(channel push payload)
    ret match {
      case Success(()) =>
        Some(enumerator)
      case Failure(t) =>
        log.warn(s"Unable to send payload: $payload", t)
        ongoingRequests -= uuid
        responseChannel.eofAndEnd()
        None
    }
  }

  def remove(uuid: UUID) = ongoingRequests remove uuid

  def exists(uuid: UUID) = ongoingRequests contains uuid
}
