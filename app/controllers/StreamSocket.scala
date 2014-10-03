package controllers

import com.mle.play.ws.WebSocketController
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.libs.json.JsValue
import play.api.mvc.WebSocket.FrameFormatter
import play.api.mvc.{Result, Results, WebSocket}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * @author Michael
 */
trait StreamSocket extends WebSocketController {
  override type Message = JsValue

  def openSocket: WebSocket[Message, Message] = ws4(FrameFormatter.jsonFrame)

  def ws4(implicit frameFormatter: WebSocket.FrameFormatter[Message]): WebSocket[Message, Message] =
    WebSocket.tryAccept[Message](req => Future.successful {
      authenticate(req).fold[Either[Result, (Iteratee[Message, _], Enumerator[Message])]](Left(Results.Unauthorized))(user => {
        val (out, channel) = Concurrent.broadcast[Message]
        val clientInfo: Client = newClient(user.user, channel)(req)
        onConnect(clientInfo)
        // iteratee that eats client messages (input)
        val in = Iteratee.foreach[Message](msg => onMessage(msg, clientInfo)).map(_ => onDisconnect(clientInfo))
        val outEnumerator = Enumerator.interleave(toEnumerator(welcomeMessage2(clientInfo)), out)
        Right((in, outEnumerator))
      })
    })

  def welcomeMessage2(client: Client): Option[Message] = None

  def toEnumerator(maybeMessage: Option[Message]) =
    maybeMessage.map(Enumerator[Message](_)).getOrElse(Enumerator.empty[Message])
}
