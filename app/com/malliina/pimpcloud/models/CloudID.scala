package com.malliina.pimpcloud.models

import com.malliina.play.json.SimpleCompanion
import play.api.data.format.Formats.stringFormat

case class CloudID(id: String)

object CloudID extends SimpleCompanion[String, CloudID] {
  override def raw(t: CloudID): String = t.id
}
