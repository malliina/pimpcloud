package com.mle.musicpimp.models

import com.mle.musicpimp.audio.Track
import play.api.libs.json.Json

/**
  * @author mle
  */
case class SavedPlaylist(id: PlaylistID, name: String, tracks: Seq[Track])

object SavedPlaylist {
  implicit val json = Json.format[SavedPlaylist]
}
