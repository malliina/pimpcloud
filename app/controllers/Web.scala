package controllers

import com.mle.concurrent.FutureOps
import com.mle.musicpimp.models.User
import com.mle.pimpcloud.CloudCredentials
import com.mle.play.controllers.{BaseSecurity, BaseController}
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import views.html

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * @author Michael
 */
class Web(servers: Servers) extends Secured with BaseSecurity with BaseController {
  val serverFormKey = "server"

  def ping = Action(NoCache(Ok))

  val cloudForm = Form[CloudCredentials](mapping(
    serverFormKey -> nonEmptyText,
    userFormKey -> nonEmptyText.transform[User](User.apply, _.name),
    passFormKey -> nonEmptyText
  )(CloudCredentials.apply)(CloudCredentials.unapply))

  def login = Action(implicit req => Ok(views.html.login(cloudForm, this)))

  def formAuthenticate = Action.async(implicit request => {
    val remoteAddress = request.remoteAddress
    cloudForm.bindFromRequest.fold(
      formWithErrors => {
        val user = formWithErrors.data.getOrElse(userFormKey, "")
        log warn s"Authentication failed for user: $user from: $remoteAddress"
        fut(BadRequest(html.login(formWithErrors, this)))
      },
      creds => {
        servers.validate(creds).map(_ => {
          val server = creds.cloudID
          val user = creds.username
          val who = s"$user@$server"
          log info s"Authentication succeeded to: $who from: $remoteAddress"
          val intendedUrl = request.session.get(INTENDED_URI) getOrElse defaultLoginSuccessPage.url
          Redirect(intendedUrl).withSession(Security.username -> server)
        }).recoverAll(t => BadRequest(html.login(cloudForm.withGlobalError("Invalid credentials."), this)))
      }
    )
  })

  def defaultLoginSuccessPage: Call = routes.Phones.rootFolder()

  def fut[T](body: => T) = Future successful body
}
