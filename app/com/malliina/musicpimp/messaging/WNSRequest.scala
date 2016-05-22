package com.malliina.musicpimp.messaging

import com.malliina.push.wns._
import play.api.libs.json.Json

case class WNSRequest(tokens: Seq[WNSToken],
                      tile: Option[TileElement],
                      toast: Option[ToastElement],
                      badge: Option[Badge],
                      raw: Option[Raw]) {
  val message: Option[WNSNotification] = tile orElse toast orElse badge orElse raw
}

object WNSRequest {

  //implicit val json = Json.format[WNSRequest]
}
