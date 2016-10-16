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

  def remove(uuid: UUID): Future[Unit]
}
