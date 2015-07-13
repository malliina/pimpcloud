package controllers

import com.mle.logbackrx.RxLogback.EventMapping
import com.mle.logbackrx.{BasicBoundedReplayRxAppender, LogbackUtils}
import com.mle.play.controllers.LogStreaming
import com.ning.http.client.AsyncHttpClientConfig
import play.api.libs.ws.ning.NingWSClient
import play.api.mvc.Call

/**
 * @author Michael
 */
object Logs extends AdminStreaming with LogStreaming {
  implicit val client = new NingWSClient(new AsyncHttpClientConfig.Builder().build())

  override def appender: EventMapping = LogbackUtils.getAppender[BasicBoundedReplayRxAppender]("RX")

  override def openSocketCall: Call = routes.Logs.openSocket()

  override def clients: Seq[Client] = subscriptions.keys.toSeq

  // Pages
  def logs = navigate(implicit req => views.html.logs())

}
