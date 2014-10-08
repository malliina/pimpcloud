package controllers

import com.mle.logbackrx.RxLogback.EventMapping
import com.mle.logbackrx.{BasicBoundedReplayRxAppender, LogbackUtils}
import com.mle.play.controllers.{LogStreaming, OAuthSecured}
import com.mle.play.ws.SyncAuth
import play.api.mvc.{Action, Call, EssentialAction, RequestHeader}

/**
 * @author Michael
 */
object Admin extends SyncAuth with LogStreaming with OAuthSecured {

  // Sockets

  override def appender: EventMapping = LogbackUtils.getAppender[BasicBoundedReplayRxAppender]("RX")

  override def openSocketCall: Call = routes.Admin.openSocket()

  override def clients: Seq[Client] = subscriptions.keys.toSeq

  // OAuth

  override val sessionUserKey: String = "email"

  override def isAuthorized(email: String): Boolean = email == "malliina123@gmail.com"

  override def startOAuth: Call = routes.Admin.initiate()

  override def oAuthRedir: Call = routes.Admin.redirResponse()

  override def onOAuthSuccess: Call = routes.Admin.logs()

  override def ejectCall: Call = routes.Admin.eject()

  // Pages
  def index = navigate(implicit req => views.html.admin())

  def logs = navigate(implicit req => views.html.logs())

  def navigate(page: => play.twirl.api.Html) = AuthAction(implicit req => Ok(page))

  def navigate(f: RequestHeader => play.twirl.api.Html): EssentialAction =
    AuthAction(implicit req => Ok(f(req)))

  def eject = Logged(Action(implicit req => Ok(views.html.eject())))

  def logout = AuthAction(implicit req => ejectWith(logoutMessage).withNewSession)
}