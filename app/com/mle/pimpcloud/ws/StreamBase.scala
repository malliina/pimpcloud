package com.mle.pimpcloud.ws

import java.util.UUID

import com.mle.musicpimp.audio.Track
import controllers.Phones
import play.api.libs.iteratee.Enumerator
import play.api.mvc.{BodyParser, MultipartFormData}

/**
 * @author Michael
 */
trait StreamBase[T] {
  def snapshot: Seq[StreamData]

  def stream(track: Track): Option[Enumerator[T]]

  def parser(uuid: UUID): Option[BodyParser[MultipartFormData[_]]]

  def exists(uuid: UUID): Boolean

  def remove(uuid: UUID) = {
    removeUUID(uuid)
    streamChanged()
  }

  protected def removeUUID(uuid: UUID): Unit

  protected def streamChanged(): Unit = {
    Phones.updateRequestList()
  }
}