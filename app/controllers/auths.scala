package controllers

import akka.stream.Materializer
import com.malliina.play.controllers.{OAuthControl, OAuthSecured}
import com.malliina.play.http.{AuthedRequest, FullRequest}
import play.api.http.Writeable
import play.api.mvc.Results.Ok
import play.api.mvc.{Call, EssentialAction, RequestHeader, Result}

import scala.concurrent.Future

class ProdAuth(ctrl: OAuthCtrl) extends PimpAuth {
  override def logged(action: EssentialAction) = ctrl.logged(action)

  override def authenticate(request: RequestHeader) = ctrl.authenticate(request)

  override def authAction(f: FullRequest => Result) = ctrl.authAction(f)

  override def eject = ctrl.eject

  override def messageKey = ctrl.messageKey
}

trait PimpAuth {
  def logged(action: EssentialAction): EssentialAction

  def authenticate(request: RequestHeader): Future[Option[AuthedRequest]]

  def authAction(f: FullRequest => Result): EssentialAction

  def eject: Result

  def messageKey: String

  def ejectAction = authAction(_ => eject.withNewSession)

  def navigate[C: Writeable](f: RequestHeader => C): EssentialAction =
    authAction(req => Ok(f(req)))
}

trait OAuthRoutes {
  def initiate: EssentialAction

  def redirResponse: EssentialAction
}

class OAuthCtrl(oauth: AdminOAuth) extends OAuthSecured(oauth, oauth.mat) {
  def initiate = oauth.initiate

  def redirResponse = oauth.redirResponse

  def eject: Result = oauth.ejectWith(oauth.logoutMessage)

  def messageKey = oauth.messageKey
}

class WebOAuthRoutes(oauth: OAuthControl) extends OAuthRoutes {
  override def initiate = oauth.initiate

  override def redirResponse = oauth.redirResponse
}

class AdminOAuth(val mat: Materializer) extends OAuthControl(mat) {
  override val sessionUserKey: String = "email"

  override def isAuthorized(email: String): Boolean = email == "malliina123@gmail.com"

  override def startOAuth: Call = routes.OAuthRoutes.initiate()

  override def oAuthRedir: Call = routes.OAuthRoutes.redirResponse()

  override def onOAuthSuccess: Call = routes.Logs.index()

  override def ejectCall: Call = routes.AdminAuth.eject()
}
