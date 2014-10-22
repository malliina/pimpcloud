package com.mle.pimpcloud.ws

import com.mle.concurrent.FutureImplicits.RichFuture
import com.mle.musicpimp.cloud.PimpSocket
import com.mle.musicpimp.json.JsonStrings.{ADDRESS, BODY, CMD, EVENT, PHONES, PLAYER, SERVER_KEY, STATUS}
import com.mle.play.concurrent.ExecutionContexts.synchronousIO
import com.mle.play.ws.{JsonWebSockets, SocketClient}
import com.mle.ws.TrieClientStorage
import controllers.{Phones, UsersEvents}
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Call, RequestHeader}

import scala.concurrent.Future

/**
 * @author Michael
 */
object PhoneSockets extends JsonWebSockets with TrieClientStorage with UsersEvents {
  override type AuthSuccess = PimpSocket
  override type Client = PhoneClient

  implicit val writer = new Writes[Client] {
    override def writes(o: Client): JsValue = Json.obj(
      SERVER_KEY -> o.connectedServer.id,
      ADDRESS -> o.req.remoteAddress
    )
  }
  val usersJson = users.map(phones => Json.obj(EVENT -> PHONES, BODY -> phones.map(Json.toJson(_)(writer))))

  override def openSocketCall: Call = routes.PhoneSockets.openSocket()

  override def authenticateAsync(req: RequestHeader): Future[AuthSuccess] = Phones.authPhone(req)

  override def newClient(authResult: AuthSuccess, channel: Channel[Message])(implicit request: RequestHeader): Client =
    PhoneClient(authResult, channel, request)

  override def onMessage(msg: Message, client: Client): Unit = {
    val isStatus = (msg \ CMD).validate[String].filter(_ == STATUS).isSuccess
    if (isStatus) {
      client.connectedServer.status
        .map(resp => client.channel push resp)
        .recoverAll(t => log.warn(s"Status request failed.", t))
    } else {
      client.connectedServer send Json.obj(CMD -> PLAYER, BODY -> msg)
    }
  }

  override def welcomeMessage(client: Client): Option[Message] = Some(com.mle.play.json.JsonMessages.welcome)
}

case class PhoneClient(connectedServer: PimpSocket, channel: Channel[JsValue], req: RequestHeader)
  extends SocketClient[JsValue]
