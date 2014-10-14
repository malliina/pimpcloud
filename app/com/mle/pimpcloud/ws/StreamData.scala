package com.mle.pimpcloud.ws

import java.util.UUID

import com.mle.musicpimp.audio.Track
import play.api.libs.json.Json

/**
 * @author Michael
 */
case class StreamData(uuid: UUID, serverID: String, track: Track)

object StreamData {
  implicit val format = Json.format[StreamData]
}