package controllers

import com.malliina.play.controllers.AccountController
import controllers.Secured.log
import play.api.Logger
import play.api.mvc.{Call, RequestHeader, Result}

trait Secured extends AccountController with PimpContentController {
  protected override def onUnauthorized(implicit request: RequestHeader): Result = {
    logUnauthorized(request)
    log debug s"Intended: ${request.uri}"
    pimpResult(request)(
      html = Redirect(loginRedirectCall).withSession(INTENDED_URI -> request.uri),
      json = Unauthorized
    )
  }

  def loginRedirectCall: Call = routes.Web.login()
}

object Secured {
  private val log = Logger(getClass)
}
