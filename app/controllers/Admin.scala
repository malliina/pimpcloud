package controllers

import com.mle.logbackrx.RxLogback.EventMapping
import com.mle.logbackrx.{BasicBoundedReplayRxAppender, LogbackUtils}
import com.mle.play.controllers.LogStreaming
import play.api.mvc.{Action, Call}

/**
 * @author Michael
 */
object Admin extends AdminStreaming with LogStreaming {

  // Sockets

  override def appender: EventMapping = LogbackUtils.getAppender[BasicBoundedReplayRxAppender]("RX")

  override def openSocketCall: Call = routes.Admin.openSocket()

  override def clients: Seq[Client] = subscriptions.keys.toSeq

  // Pages

  def index = navigate(implicit req => views.html.admin())

  def logs = navigate(implicit req => views.html.logs())

  def eject = Logged(Action(implicit req => Ok(views.html.eject())))
}