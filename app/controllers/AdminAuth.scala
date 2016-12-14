package controllers

import akka.stream.Materializer
import com.malliina.play.controllers.{OAuthControl, OAuthSecured}
import play.api.http.Writeable
import play.api.mvc.Results.Ok
import play.api.mvc.{Action, EssentialAction, RequestHeader}

class AdminAuth(oauth: OAuthControl, tags: CloudTags, mat: Materializer) extends OAuthSecured(oauth, mat) {
  def this(tags: CloudTags, mat: Materializer) = this(new AdminOAuth(mat), tags, mat)

  def initiate = oauth.initiate

  def redirResponse = oauth.redirResponse

  // HTML
  def logout = authAction(_ => oauth.ejectWith(oauth.logoutMessage).withNewSession)

  def eject = logged(Action(req => Ok(tags.eject(req.flash.get(oauth.messageKey)))))

  def navigate[C: Writeable](f: RequestHeader => C): EssentialAction =
    authAction(req => Ok(f(req)))
}
