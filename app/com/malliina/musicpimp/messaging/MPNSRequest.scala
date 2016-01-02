package com.malliina.musicpimp.messaging

import com.malliina.push.mpns._
import play.api.libs.json.Json

/**
  * @author mle
  */
case class MPNSRequest(tokens: Seq[MPNSToken],
                       toast: Option[ToastMessage],
                       tile: Option[TileData],
                       flip: Option[FlipData],
                       iconic: Option[IconicData],
                       cycle: Option[CycleTile]) {
  val message: Option[MPNSMessage] = toast orElse tile orElse flip orElse iconic orElse cycle
}

object MPNSRequest {
  implicit val tileJson = Json.format[TileData]
  implicit val (toastJson, flipJson, iconicJson, cycleJson) =
    (Json.format[ToastMessage], Json.format[FlipData],
      Json.format[IconicData], Json.format[CycleTile])
  implicit val json = Json.format[MPNSRequest]
}
