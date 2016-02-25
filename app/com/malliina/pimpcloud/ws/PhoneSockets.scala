package com.malliina.pimpcloud.ws

import akka.actor.ActorSystem
import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.json.JsonStrings
import com.malliina.musicpimp.json.JsonStrings.{ADDRESS, BODY, CMD, EVENT, PHONES, SERVER_KEY, STATUS}
import com.malliina.pimpcloud.actors.ActorStorage
import com.malliina.pimpcloud.ws.PhoneSockets.log
import com.malliina.play.ws.SocketClient
import com.malliina.ws.PhoneActorSockets
import controllers.PhoneConnection
import play.api.Logger
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.{JsObject, JsValue, Json, Writes}
import play.api.mvc.{Call, RequestHeader}
import rx.lang.scala.Observable

import scala.concurrent.Future

abstract class PhoneSockets(actorSystem: ActorSystem) extends PhoneActorSockets(ActorStorage.phones(actorSystem)) {
  override type AuthSuccess = PhoneConnection

  implicit val writer = Writes[PhoneClient](o => Json.obj(
    SERVER_KEY -> o.connectedServer.id,
    ADDRESS -> o.req.remoteAddress
  ))

  val usersJson: Observable[JsObject] = storage.users.map(phoneClients => Json.obj(EVENT -> PHONES, BODY -> phoneClients))

  override def openSocketCall: Call = routes.PhoneSockets.openSocket()

  override def authenticateAsync(req: RequestHeader): Future[AuthSuccess] = authenticatePhone(req)

  def authenticatePhone(req: RequestHeader): Future[AuthSuccess]

  override def newClient(authResult: AuthSuccess, channel: Channel[JsValue])(implicit request: RequestHeader): PhoneClient =
    PhoneClient(authResult, channel, request)

  override def onMessage(msg: JsValue, client: PhoneClient): Boolean = {
    val isStatus = (msg \ CMD).validate[String].filter(_ == STATUS).isSuccess
    if (isStatus) {
      client.connectedServer.status
        .map(resp => client.channel push resp)
        .recoverAll(t => log.warn(s"Status request failed.", t))
    } else {
      client.connectedServer send Json.obj(CMD -> JsonStrings.PLAYER, BODY -> msg)
    }
    true
  }

  override def welcomeMessage(client: PhoneClient): Option[JsValue] = Some(com.malliina.play.json.JsonMessages.welcome)
}

object PhoneSockets {
  private val log = Logger(getClass)
}

case class PhoneClient(connection: PhoneConnection, channel: Channel[JsValue], req: RequestHeader)
  extends SocketClient[JsValue] {
  val phoneUser = connection.user
  val connectedServer = connection.server
}
