package com.malliina.pimpcloud.ws

import akka.stream.Materializer
import akka.stream.scaladsl.SourceQueue
import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.musicpimp.json.JsonStrings
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.pimpcloud.ws.PhoneSockets.log
import com.malliina.play.ws.SocketClient
import com.malliina.ws.{PhoneActorSockets, RxStmStorage}
import controllers.PhoneConnection
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json, Writes}
import play.api.mvc.{Call, RequestHeader}
import rx.lang.scala.Observable

import scala.concurrent.Future

abstract class PhoneSockets(val storage: RxStmStorage[PhoneClient], val mat: Materializer)
  extends PhoneActorSockets {
  override type Client = PhoneClient
  override type AuthSuccess = PhoneConnection

  implicit val writer = Writes[PhoneClient](o => Json.obj(
    SERVER_KEY -> o.connectedServer.id,
    ADDRESS -> o.req.remoteAddress
  ))

  def send(message: Message, from: PimpServerSocket) =
    Future.traverse(clients.filter(_.connectedServer == from))(_.channel.offer(message))

  val usersJson: Observable[JsObject] = storage.users.map(phoneClients => Json.obj(EVENT -> PHONES, BODY -> phoneClients))

  override def openSocketCall: Call = routes.PhoneSockets.openSocket()

  override def authenticateAsync(req: RequestHeader): Future[AuthSuccess] = authenticatePhone(req)

  def authenticatePhone(req: RequestHeader): Future[AuthSuccess]

  override def newClient(authResult: AuthSuccess, channel: SourceQueue[JsValue])(implicit request: RequestHeader): PhoneClient =
    PhoneClient(authResult, channel, request)

  override def onMessage(msg: Message, client: PhoneClient): Boolean = {
    val isStatus = (msg \ CMD).validate[String].filter(_ == STATUS).isSuccess
    if (isStatus) {
      client.connectedServer.status
        .flatMap(resp => client.channel offer resp)
        .recoverAll(t => log.warn(s"Status request failed.", t))
    } else {
      client.connectedServer send Json.obj(CMD -> JsonStrings.PLAYER, BODY -> msg)
    }
    true
  }

  override def welcomeMessage(client: Client): Option[JsValue] =
    Some(com.malliina.play.json.JsonMessages.welcome)
}

object PhoneSockets {
  private val log = Logger(getClass)
}

case class PhoneClient(connection: PhoneConnection, channel: SourceQueue[JsValue], req: RequestHeader)
  extends SocketClient[JsValue] {
  val phoneUser = connection.user
  val connectedServer = connection.server
}
