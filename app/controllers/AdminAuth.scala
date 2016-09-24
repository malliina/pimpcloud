package controllers

import akka.stream.Materializer
import com.malliina.play.controllers.{OAuthControl, OAuthSecured}
import play.api.mvc.{Action, Results}
import views.html

class AdminAuth(oauth: OAuthControl, mat: Materializer) extends OAuthSecured(oauth, mat) {
  def this(mat: Materializer) = this(new AdminOAuth(mat), mat)

  def initiate = oauth.initiate

  def redirResponse = oauth.redirResponse

  // HTML
  def logout = authAction(req => oauth.ejectWith(oauth.logoutMessage).withNewSession)

  def eject = logged(Action(req => Results.Ok(html.eject(oauth.messageKey)(req.flash))))

}
