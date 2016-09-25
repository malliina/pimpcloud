package controllers

import com.malliina.logbackrx.RxLogback.EventMapping
import com.malliina.logbackrx.{BasicBoundedReplayRxAppender, LogEvent, LogbackUtils}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Call
import rx.lang.scala.Observable

import scala.concurrent.duration.DurationInt

class Logs(admin: AdminAuth) extends AdminStreaming(admin) {
  def logEvents: Observable[LogEvent] = appender.logEvents

  override lazy val jsonEvents: Observable[JsValue] =
    logEvents.tumblingBuffer(100.millis).filter(_.nonEmpty).map(Json.toJson(_))

  def appender: EventMapping =
    LogbackUtils.getAppender[BasicBoundedReplayRxAppender]("RX")

  override def openSocketCall: Call = routes.Logs.openSocket()

  def logs = admin.navigate(req => views.html.logs(wsUrl(req)))
}
