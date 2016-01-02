package com.malliina.musicpimp.messaging

import com.malliina.push.gcm.{GCMResponse, MappedGCMResponse}
import play.api.libs.json.Json

/**
  * @author mle
  */
case class PushResult(apns: Seq[APNSResult],
                      gcm: Seq[MappedGCMResponse],
                      adm: Seq[BasicResult],
                      mpns: Seq[BasicResult])

object PushResult {
  // TODO add these two to mobile-push
  implicit val gcmResponseJson = Json.writes[GCMResponse]
  implicit val mappedGcmResponseJson = Json.writes[MappedGCMResponse]

  implicit val json = Json.format[PushResult]
}
