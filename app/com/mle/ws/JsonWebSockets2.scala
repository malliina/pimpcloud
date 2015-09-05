package com.mle.ws

import com.mle.play.json.JsonMessages
import play.api.libs.json.JsValue
import play.api.mvc.WebSocket
import play.api.mvc.WebSocket.FrameFormatter
import rx.lang.scala.Observable

import scala.concurrent.duration.DurationInt

/**
 * @author mle
 */
trait JsonWebSockets2[C <: com.mle.play.ws.SocketClient[JsValue]] extends WSController2[JsValue, C] {
  // prevents connections being dropped after 30s of inactivity; i don't know how to modify that timeout
  val pinger = Observable.interval(20.seconds).subscribe(_ => broadcast(JsonMessages.ping))

  def openSocket: WebSocket[JsValue, JsValue] = ws(FrameFormatter.jsonFrame)

  def openSocket2: WebSocket[JsValue, JsValue] = ws2(FrameFormatter.jsonFrame)
}
