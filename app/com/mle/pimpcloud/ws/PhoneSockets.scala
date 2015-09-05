package com.mle.pimpcloud.ws

import com.mle.concurrent.ExecutionContexts.cached
import com.mle.concurrent.FutureOps
import com.mle.musicpimp.cloud.PimpSocket
import com.mle.musicpimp.json.JsonStrings.{ADDRESS, BODY, CMD, EVENT, PHONES, PLAYER, SERVER_KEY, STATUS}
import com.mle.play.ws.{JsonWebSockets, SocketClient}
import com.mle.ws.TrieClientStorage
import controllers.{Servers, UsersEvents}
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Call, RequestHeader}

import scala.concurrent.Future

/**
 * @author Michael
 */
trait PhoneSockets extends JsonWebSockets with TrieClientStorage with UsersEvents {
  override type AuthSuccess = PimpSocket
  override type Client = PhoneClient

  implicit val writer = Writes[Client](o => Json.obj(
    SERVER_KEY -> o.connectedServer.id,
    ADDRESS -> o.req.remoteAddress
  ))

  val usersJson = users.map(phoneClients => Json.obj(EVENT -> PHONES, BODY -> phoneClients))

  override def openSocketCall: Call = routes.PhoneSockets.openSocket()

  override def authenticateAsync(req: RequestHeader): Future[AuthSuccess] = authenticatePhone(req)

  def authenticatePhone(req: RequestHeader): Future[AuthSuccess]

  override def newClient(authResult: AuthSuccess, channel: Channel[Message])(implicit request: RequestHeader): Client =
    PhoneClient(authResult, channel, request)

  override def onMessage(msg: Message, client: PhoneClient): Boolean = {
    val isStatus = (msg \ CMD).validate[String].filter(_ == STATUS).isSuccess
    if (isStatus) {
      client.connectedServer.status
        .map(resp => client.channel push resp)
        .recoverAll(t => log.warn(s"Status request failed.", t))
    } else {
      client.connectedServer send Json.obj(CMD -> PLAYER, BODY -> msg)
    }
    true
  }

  override def welcomeMessage(client: Client): Option[Message] = Some(com.mle.play.json.JsonMessages.welcome)
}

case class PhoneClient(connectedServer: PimpSocket, channel: Channel[JsValue], req: RequestHeader)
  extends SocketClient[JsValue]
