package com.malliina.musicpimp.audio

import play.api.libs.json.Json

case class Folder(id: String, title: String) extends MusicItem

object Folder {
  implicit val json = Json.format[Folder]
}
