package com.malliina.pimpcloud.auth

import com.malliina.pimpcloud.CloudCredentials
import controllers.{PhoneConnection, Server}
import play.api.mvc.RequestHeader

import scala.concurrent.Future

trait CloudAuthentication {
  def authServer(req: RequestHeader): Future[Server]

  def authPhone(req: RequestHeader): Future[PhoneConnection]

  def validate(creds: CloudCredentials): Future[PhoneConnection]
}
