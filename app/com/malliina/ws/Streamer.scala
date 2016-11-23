package com.malliina.ws

import java.util.UUID

import com.malliina.storage.StorageInt
import play.api.mvc.{BodyParser, MultipartFormData}

import scala.concurrent.Future

object Streamer {
  val DefaultMaxUploadSize = 1024.megs
}

trait Streamer {
  val maxUploadSize = Streamer.DefaultMaxUploadSize

  def parser(uuid: UUID): Option[BodyParser[MultipartFormData[Long]]]

  /**
    *
    * @param uuid request ID
    * @param isCanceled if true, the server is informed that it should cancel the request
    * @return
    */
  def remove(uuid: UUID, isCanceled: Boolean): Future[Unit]
}
