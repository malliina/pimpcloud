package com.malliina.pimpcloud

import com.malliina.musicpimp.models.User
import com.malliina.play.auth.Auth
import play.api.mvc.RequestHeader

object PimpAuth {
  def cloudCredentials(request: RequestHeader): Option[CloudCredentials] = {
    Auth.authHeaderParser(request)(decoded => {
      decoded.split(":", 3) match {
        case Array(cloudID, user, pass) => Some(CloudCredentials(cloudID, User(user), pass))
        case _ => None
      }
    })
  }
}

case class CloudCredentials(cloudID: String, username: User, password: String)
