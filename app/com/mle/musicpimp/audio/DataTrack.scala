package com.mle.musicpimp.audio

import com.mle.json.JsonFormats
import com.mle.storage.StorageSize
import controllers.Phones
import play.api.libs.json.Json

import scala.concurrent.duration.Duration

/**
 * @author Michael
 */
case class DataTrack(id: String, title: String, artist: String, album: String, duration: Duration, size: StorageSize, folder: String) {
  val path = Phones decode folder
}

object DataTrack {
  implicit val durJson = JsonFormats.durationFormat
  implicit val storageJson = JsonFormats.storageSizeFormat
  implicit val json = Json.format[DataTrack]
}