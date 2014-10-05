package controllers

import com.mle.play.ws.WebSocketController
import com.mle.util.Log
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.libs.json.JsValue
import play.api.mvc.WebSocket.FrameFormatter
import play.api.mvc.{RequestHeader, Result, Results, WebSocket}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * @author Michael
 */
trait StreamSocket extends WebSocketController with Log {
  override type Message = JsValue

  def openSocket: WebSocket[Message, Message] = ws4(FrameFormatter.jsonFrame)

  def openSocket2: WebSocket[Message, Message] = ws5(FrameFormatter.jsonFrame)

  def ws4(implicit frameFormatter: WebSocket.FrameFormatter[Message]): WebSocket[Message, Message] =
    WebSocket.tryAccept[Message](req => Future.successful {
      authenticate(req).fold[Either[Result, (Iteratee[Message, _], Enumerator[Message])]](Left(onUnauthorized(req)))(user => {
        val (out, channel) = Concurrent.broadcast[Message]
        val clientInfo: Client = newClient(user.user, channel)(req)
        onConnect(clientInfo)
        // iteratee that eats client messages (input)
        val in = Iteratee.foreach[Message](msg => onMessage(msg, clientInfo)).map(_ => onDisconnect(clientInfo))
        val outEnumerator = Enumerator.interleave(toEnumerator(welcomeMessage2(clientInfo)), out)
        Right((in, outEnumerator))
      })
    })

  /**
   * The Java-WebSocket client library hangs if an Unauthorized result is returned after a websocket connection attempt.
   *
   * So instead of returning an Unauthorized result this opens then immediately closes unauthorized connections, and
   * ignores any messages.
   *
   * @param frameFormatter
   * @return
   */
  def ws5(implicit frameFormatter: WebSocket.FrameFormatter[Message]): WebSocket[Message, Message] =
    WebSocket.using[Message](request => {
      authenticate(request).map(user => {
        val (out, channel) = Concurrent.broadcast[Message]
        val clientInfo: Client = newClient(user.user, channel)(request)
        onConnect(clientInfo)
        // iteratee that eats client messages (input)
        val in = Iteratee.foreach[Message](msg => onMessage(msg, clientInfo)).map(_ => onDisconnect(clientInfo))
        val enumerator = Enumerator.interleave(toEnumerator(welcomeMessage2(clientInfo)), out)
        (in, enumerator)
      }).getOrElse({
        // authentication failed
        onUnauthorized(request)
        val in = Iteratee.foreach[Message](_ => ())
        val out = Enumerator.eof[Message]
        (in, out)
      })
    })

  def onUnauthorized(req: RequestHeader): Result = {
    log warn s"Unauthorized websocket connection attempt from: ${req.remoteAddress} to: ${req.path}"
    Results.Unauthorized
  }

  def welcomeMessage2(client: Client): Option[Message] = None

  def toEnumerator(maybeMessage: Option[Message]) =
    maybeMessage.map(Enumerator[Message](_)).getOrElse(Enumerator.empty[Message])
}
