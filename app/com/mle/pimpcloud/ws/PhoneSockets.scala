package com.mle.pimpcloud.ws

import com.mle.musicpimp.cloud.PimpSocket
import com.mle.play.ws.{JsonWebSockets, SocketClient}
import com.mle.ws.TrieClientStorage
import controllers.Phones
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.JsValue
import play.api.mvc.{Call, RequestHeader}

import scala.concurrent.Future

/**
 * @author Michael
 */
object PhoneSockets extends JsonWebSockets with TrieClientStorage {
  override type AuthSuccess = PimpSocket
  override type Client = PhoneClient

  override def openSocketCall: Call = routes.PhoneSockets.openSocket()

  override def authenticateAsync(req: RequestHeader): Future[AuthSuccess] = Phones.authPhone(req)

  override def newClient(authResult: AuthSuccess, channel: Channel[Message])(implicit request: RequestHeader): Client =
    PhoneClient(authResult, channel, request)

  override def onMessage(msg: Message, client: Client): Unit = {
    client.connectedServer send msg
  }

  override def welcomeMessage(client: Client): Option[Message] = Some(com.mle.play.json.JsonMessages.welcome)
}

case class PhoneClient(connectedServer: PimpSocket, channel: Channel[JsValue], req: RequestHeader)
  extends SocketClient[JsValue]
