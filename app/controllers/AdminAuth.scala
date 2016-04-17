package controllers

import akka.stream.Materializer
import com.malliina.play.controllers.OAuthSecured
import play.api.mvc.{Action, Call}

class AdminAuth(val mat: Materializer) extends OAuthSecured {
  // OAuth
  override val sessionUserKey: String = "email"

  override def isAuthorized(email: String): Boolean = email == "malliina123@gmail.com"

  override def startOAuth: Call = routes.AdminAuth.initiate()

  override def oAuthRedir: Call = routes.AdminAuth.redirResponse()

  override def onOAuthSuccess: Call = routes.UsageStreaming.index()

  override def ejectCall: Call = routes.AdminAuth.eject()

  // HTML
  def logout = AuthAction(implicit req => ejectWith(logoutMessage).withNewSession)

  def eject = Logged(Action(implicit req => Ok(views.html.eject(messageKey))))
}
