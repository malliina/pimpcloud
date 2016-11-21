package controllers

import akka.stream.Materializer
import com.malliina.play.controllers.OAuthControl
import play.api.mvc.Call

class AdminOAuth(mat: Materializer) extends OAuthControl(mat) {
  // OAuth
  override val sessionUserKey: String = "email"

  override def isAuthorized(email: String): Boolean = email == "malliina123@gmail.com"

  override def startOAuth: Call = routes.AdminAuth.initiate()

  override def oAuthRedir: Call = routes.AdminAuth.redirResponse()

  override def onOAuthSuccess: Call = routes.UsageStreaming.index()

  override def ejectCall: Call = routes.AdminAuth.eject()
}