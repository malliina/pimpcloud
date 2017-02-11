package controllers

import akka.stream.Materializer
import play.api.http.Writeable
import play.api.mvc.Results.Ok
import play.api.mvc.{Action, EssentialAction, RequestHeader}

/** TODO remove this code; seems to wrap unnecessarily.
  */
class AdminAuth(oauth: PimpAuth, tags: CloudTags, val mat: Materializer) {
  // HTML
  def logout = oauth.ejectAction

  def eject = oauth.logged(Action(req => Ok(tags.eject(req.flash.get(oauth.messageKey)))))

  def navigate[C: Writeable](f: RequestHeader => C): EssentialAction =
    oauth.authAction(req => Ok(f(req)))
}
