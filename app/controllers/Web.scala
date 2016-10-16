package controllers

import com.malliina.concurrent.FutureOps
import com.malliina.pimpcloud.CloudCredentials
import com.malliina.pimpcloud.auth.CloudAuthentication
import com.malliina.pimpcloud.models.CloudID
import com.malliina.play.controllers.{AccountForms, BaseController}
import com.malliina.play.models.{Password, Username}
import controllers.Web.log
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import views.html

import scala.concurrent.{ExecutionContext, Future}

class Web(authActions: CloudAuthentication, exec: ExecutionContext, val forms: AccountForms)
  extends BaseController
    with Controller {

  val serverFormKey = "server"

  val cloudForm = Form[CloudCredentials](mapping(
    serverFormKey -> CloudID.mapping,
    forms.userFormKey -> Username.mapping,
    forms.passFormKey -> Password.mapping
  )(CloudCredentials.apply)(CloudCredentials.unapply))

  def ping = Action(NoCache(Ok))

  def login = Action(req => Ok(views.html.login(cloudForm, this, req.flash)))

  def formAuthenticate = Action.async { request =>
    val flash = request.flash
    val remoteAddress = request.remoteAddress
    cloudForm.bindFromRequest()(request).fold(
      formWithErrors => {
        val user = formWithErrors.data.getOrElse(forms.userFormKey, "")
        log warn s"Authentication failed for user: $user from: $remoteAddress"
        fut(BadRequest(html.login(formWithErrors, this, flash)))
      },
      creds => {
        implicit val ec = exec
        authActions.validate(creds).map(_ => {
          val server = creds.cloudID
          val user = creds.username
          val who = s"$user@$server"
          log info s"Authentication succeeded to: $who from: $remoteAddress"
          val intendedUrl = request.session.get(forms.intendedUri) getOrElse defaultLoginSuccessPage.url
          Redirect(intendedUrl).withSession(Security.username -> server.id)
        }).recoverAll(t => BadRequest(html.login(cloudForm.withGlobalError("Invalid credentials."), this, flash)))
      }
    )
  }

  def defaultLoginSuccessPage: Call = routes.Phones.rootFolder()

  def fut[T](body: => T) = Future successful body
}

object Web {
  private val log = Logger(getClass)
}
