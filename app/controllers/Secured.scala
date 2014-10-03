package controllers

import com.mle.play.controllers.AccountController
import com.mle.util.Log
import play.api.mvc._

/**
 *
 * @author mle
 */
trait Secured extends AccountController with Log {
  val INTENDED_URI = "intended_uri"

  protected def logUnauthorized(implicit request: RequestHeader) {
    log warn "Unauthorized request: " + request.path + " from: " + request.remoteAddress
  }

  protected override def onUnauthorized(implicit request: RequestHeader): Result = {
    logUnauthorized(request)
    log info s"Intended: ${request.uri}"
    Redirect(loginRedirectCall).withSession(INTENDED_URI -> request.uri)
  }

  def loginRedirectCall: Call = routes.Web.login()
}