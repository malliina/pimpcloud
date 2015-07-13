package controllers

import com.mle.play.controllers.{AuthResult, Streaming}
import com.mle.play.ws.SyncAuth
import play.api.mvc.{Controller, EssentialAction, RequestHeader}

/**
 * @author Michael
 */
trait AdminStreaming extends Controller with Streaming with SyncAuth {
  override def authenticate(implicit req: RequestHeader): Option[AuthResult] = AdminAuth.authenticate(req)

  def navigate(page: => play.twirl.api.Html): EssentialAction =
    navigate(_ => page)

  def navigate(f: RequestHeader => play.twirl.api.Html): EssentialAction =
    AdminAuth.AuthAction(implicit req => Ok(f(req)))
}
