package controllers

import com.mle.logbackrx.RxLogback.EventMapping
import com.mle.logbackrx.{BasicBoundedReplayRxAppender, LogbackUtils}
import com.mle.play.controllers.LogStreaming
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Call
import rx.lang.scala.Observable

import scala.concurrent.duration.DurationInt

/**
 * @author Michael
 */
object Logs extends AdminStreaming with LogStreaming {
  override lazy val jsonEvents: Observable[JsValue] = logEvents.tumblingBuffer(100.millis).filter(_.nonEmpty).map(Json.toJson(_))

  override def appender: EventMapping = LogbackUtils.getAppender[BasicBoundedReplayRxAppender]("RX")

  override def openSocketCall: Call = routes.Logs.openSocket()

  override def clients: Seq[Client] = subscriptions.keys.toSeq

  def logs = navigate(implicit req => views.html.logs())
}
