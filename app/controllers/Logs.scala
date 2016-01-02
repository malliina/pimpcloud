package controllers

import com.malliina.logbackrx.RxLogback.EventMapping
import com.malliina.logbackrx.{BasicBoundedReplayRxAppender, LogbackUtils}
import com.malliina.play.controllers.LogStreaming
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Call
import rx.lang.scala.Observable

import scala.concurrent.duration.DurationInt

/**
 * @author Michael
 */
class Logs(adminAuth: AdminAuth) extends AdminStreaming(adminAuth) with LogStreaming {
  override lazy val jsonEvents: Observable[JsValue] =
    logEvents.tumblingBuffer(100.millis).filter(_.nonEmpty).map(Json.toJson(_))

  override def appender: EventMapping =
    LogbackUtils.getAppender[BasicBoundedReplayRxAppender]("RX")

  override def openSocketCall: Call = routes.Logs.openSocket()

  override def clients: Seq[Client] = subscriptions.keys

  def logs = navigate(implicit req => views.html.logs(wsUrl(req)))
}
