package com.mle.pimpcloud

import com.mle.play.auth.Auth
import play.api.mvc.RequestHeader

/**
 * @author Michael
 */
object PimpAuth {
  def cloudCredentials(request: RequestHeader): Option[CloudCredentials] = {
    Auth.authHeaderParser(request)(decoded => {
      decoded.split(":", 3) match {
        case Array(cloudID, user, pass) => Some(CloudCredentials(cloudID, user, pass))
        case _ => None
      }
    })
  }
}

case class CloudCredentials(cloudID: String, username: String, password: String)