package com.mle.musicpimp.audio

import play.api.libs.json.Json

/**
 *
 * @author mle
 */
case class Folder(id: String, title: String) extends MusicItem

object Folder {
  implicit val json = Json.format[Folder]
}