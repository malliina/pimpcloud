package controllers

import com.mle.play.controllers.{OAuthSecured, Streaming}
import com.mle.play.ws.SyncAuth
import play.api.mvc.{Call, EssentialAction, RequestHeader}

/**
 * @author Michael
 */
trait AdminStreaming extends Streaming with OAuthSecured with SyncAuth {
  // OAuth
  override val sessionUserKey: String = "email"

  override def isAuthorized(email: String): Boolean = email == "malliina123@gmail.com"

  override def startOAuth: Call = routes.Logs.initiate()

  override def oAuthRedir: Call = routes.Logs.redirResponse()

  override def onOAuthSuccess: Call = routes.UsageStreaming.index()

  override def ejectCall: Call = routes.Logs.eject()

  // HTML
  def logout = AuthAction(implicit req => ejectWith(logoutMessage).withNewSession)

  def navigate(page: => play.twirl.api.Html) = AuthAction(implicit req => Ok(page))

  def navigate(f: RequestHeader => play.twirl.api.Html): EssentialAction =
    AuthAction(implicit req => Ok(f(req)))
}
