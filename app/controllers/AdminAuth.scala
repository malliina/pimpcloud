package controllers

import akka.stream.Materializer
import com.malliina.play.controllers.{OAuthControl, OAuthSecured}
import play.api.mvc.Results.Ok
import play.api.mvc.{Action, EssentialAction, RequestHeader}
import play.twirl.api.Html
import views.html

class AdminAuth(oauth: OAuthControl, mat: Materializer) extends OAuthSecured(oauth, mat) {
  def this(mat: Materializer) = this(new AdminOAuth(mat), mat)

  def initiate = oauth.initiate

  def redirResponse = oauth.redirResponse

  // HTML
  def logout = authAction(req => oauth.ejectWith(oauth.logoutMessage).withNewSession)

  def eject = logged(Action(req => Ok(html.eject(oauth.messageKey)(req.flash))))

  def navigate(f: RequestHeader => Html): EssentialAction =
    authAction(req => Ok(f(req)))
}
