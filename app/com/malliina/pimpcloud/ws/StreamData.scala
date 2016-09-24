package com.malliina.pimpcloud.ws

import java.util.UUID

import com.malliina.musicpimp.audio.Track
import com.malliina.play.ContentRange
import com.malliina.play.models.Username
import play.api.libs.json.Json

case class StreamData(uuid: UUID, serverID: Username, track: Track, range: ContentRange)

object StreamData {
  implicit val format = Json.format[StreamData]
}
