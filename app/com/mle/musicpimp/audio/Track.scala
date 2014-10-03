package com.mle.musicpimp.audio

import com.mle.json.JsonFormats
import com.mle.storage.StorageSize
import play.api.libs.json.Json

import scala.concurrent.duration.Duration

case class Track(id: String,
                 title: String,
                 album: String,
                 artist: String,
                 duration: Duration,
                 size: StorageSize) extends MusicItem

object Track {
  implicit val storageJson = JsonFormats.storageSizeFormat
  implicit val dur = JsonFormats.durationFormat
  implicit val format = Json.format[Track]
}