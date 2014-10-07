package com.mle.ws

import java.util.UUID

import com.mle.musicpimp.json.JsonStrings._
import com.mle.util.Log
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.libs.json.{JsValue, Json}

import scala.collection.concurrent.TrieMap
import scala.util.{Failure, Success, Try}

/**
 * @author Michael
 */
class IterateeStore[T] extends Log {
  val ongoingRequests = TrieMap.empty[UUID, Iteratee[T, Unit]]

  def send(message: JsValue, channel: Channel[JsValue]): Option[Enumerator[T]] = {
    val (iteratee, enumerator) = Concurrent.joined[T]
    val uuid = UUID.randomUUID()
    ongoingRequests += (uuid -> iteratee)
    val payload = Json.obj(REQUEST_ID -> uuid.toString, BODY -> message)
    log info s"Sending request: $uuid with body: $message"
    val ret = Try(channel push payload)
    ret match {
      case Success(()) =>
        Some(enumerator)
      case Failure(t) =>
        log.warn(s"Unable to send payload: $payload", t)
        ongoingRequests -= uuid
        // close something?
        None
    }
  }

  def remove(uuid: UUID) = ongoingRequests remove uuid

  def exists(uuid: UUID) = ongoingRequests contains uuid
}
