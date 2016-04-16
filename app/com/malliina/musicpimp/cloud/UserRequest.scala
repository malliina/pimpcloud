package com.malliina.musicpimp.cloud

import java.util.UUID

import com.malliina.musicpimp.models.User
import play.api.libs.json.{JsValue, Json}

case class UserRequest(cmd: String, body: JsValue, request: UUID, username: User)

object UserRequest {
  implicit val json = Json.format[UserRequest]
}
