package controllers

import com.mle.play.controllers.{AuthResult, Streaming}
import com.mle.play.ws.SyncAuth
import play.api.mvc.{Controller, EssentialAction, RequestHeader}
import play.twirl.api.Html

/**
 * @author Michael
 */
abstract class AdminStreaming(adminAuth: AdminAuth) extends Controller with Streaming with SyncAuth {

  override def authenticate(implicit req: RequestHeader): Option[AuthResult] = adminAuth.authenticate(req)

  def navigate(page: => Html): EssentialAction =
    navigate(_ => page)

  def navigate(f: RequestHeader => Html): EssentialAction =
    adminAuth.AuthAction(implicit req => Ok(f(req)))
}
