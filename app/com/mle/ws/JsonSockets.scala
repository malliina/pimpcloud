package com.mle.ws

import com.mle.concurrent.FutureImplicits.RichFuture
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.libs.json.JsValue
import play.api.mvc.WebSocket.FrameFormatter
import play.api.mvc.{Call, RequestHeader, Results, WebSocket}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 *
 * @author Michael
 */
trait JsonSockets extends WebSocketBase2 {
  override type Message = JsValue

  def authenticate(req: RequestHeader): Future[AuthResult]

  def openSocketCall: Call

  def wsUrl(implicit request: RequestHeader): String = openSocketCall.webSocketURL(request.secure)

  def openSocket: WebSocket[Message, Message] = ws(FrameFormatter.jsonFrame)

  def ws(implicit frameFormatter: WebSocket.FrameFormatter[Message]): WebSocket[Message, Message] =
    WebSocket.tryAccept[Message](req => authenticate(req).map(authResult => {
      val (out, channel) = Concurrent.broadcast[Message]
      val clientInfo: Client = newClient(authResult, channel)(req)
      onConnect(clientInfo)
      // iteratee that eats client messages (input)
      val in = Iteratee.foreach[Message](msg => onMessage(msg, clientInfo)).map(_ => onDisconnect(clientInfo))
      val outEnumerator = Enumerator.interleave(toEnumerator(welcomeMessage(clientInfo)), out)
      Right((in, outEnumerator))
    }).recoverAll(t => Left(Results.Unauthorized)))

  def welcomeMessage(client: Client): Option[Message] = None

  def toEnumerator[T](maybe: Option[T]) = maybe.map(Enumerator[T](_)) getOrElse Enumerator.empty[T]
}
