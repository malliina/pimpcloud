package controllers

import com.malliina.concurrent.FutureOps
import com.malliina.pimpcloud.CloudCredentials
import com.malliina.play.controllers.BaseController
import com.malliina.play.models.Username
import controllers.Web.log
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import views.html

import scala.concurrent.Future

class Web(servers: Servers, auth: CloudAuth)
  extends Secured(auth)
    with BaseController
    with Controller {

  val serverFormKey = "server"

  def ping = Action(NoCache(Ok))

  val cloudForm = Form[CloudCredentials](mapping(
    serverFormKey -> nonEmptyText,
    userFormKey -> nonEmptyText.transform[Username](Username.apply, _.name),
    passFormKey -> nonEmptyText
  )(CloudCredentials.apply)(CloudCredentials.unapply))

  def login = Action(req => Ok(views.html.login(cloudForm, this, req.flash)))

  def formAuthenticate = Action.async { request =>
    val flash = request.flash
    val remoteAddress = request.remoteAddress
    cloudForm.bindFromRequest()(request).fold(
      formWithErrors => {
        val user = formWithErrors.data.getOrElse(userFormKey, "")
        log warn s"Authentication failed for user: $user from: $remoteAddress"
        fut(BadRequest(html.login(formWithErrors, this, flash)))
      },
      creds => {
        implicit val ec = auth.mat.executionContext
        servers.validate(creds).map(_ => {
          val server = creds.cloudID
          val user = creds.username
          val who = s"$user@$server"
          log info s"Authentication succeeded to: $who from: $remoteAddress"
          val intendedUrl = request.session.get(intendedUri) getOrElse defaultLoginSuccessPage.url
          Redirect(intendedUrl).withSession(Security.username -> server)
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
