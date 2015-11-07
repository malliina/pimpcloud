package com.mle.musicpimp.cloud

import java.util.UUID

import com.mle.musicpimp.models.User
import play.api.libs.json.{JsValue, Json}

/**
  * @author mle
  */
case class UserRequest(cmd: String, body: JsValue, request: UUID, username: User)

object UserRequest {
  implicit val json = Json.format[UserRequest]
}
