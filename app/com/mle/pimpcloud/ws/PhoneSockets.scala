package com.mle.pimpcloud.ws

import com.mle.musicpimp.cloud.PimpSocket
import com.mle.musicpimp.json.JsonStrings.{EVENT, PING}
import com.mle.ws.{JsonSockets, TrieClientStorage}
import controllers.Phones
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Call, RequestHeader}
import rx.lang.scala.Observable

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

/**
 * @author Michael
 */
object PhoneSockets extends JsonSockets with TrieClientStorage {
  val pingMessage = Json.obj(EVENT -> PING)
  val pinger = Observable.interval(20.seconds).subscribe(_ => broadcast(pingMessage))

  override def broadcast(message: PhoneSockets.Message): Unit = clients.foreach(_.phoneChannel push message)

  override type AuthResult = PimpSocket
  override type Client = PhoneClient

  override def openSocketCall: Call = routes.PhoneSockets.openSocket()

  override def authenticate(req: RequestHeader): Future[AuthResult] = Phones.authPhone(req)

  override def newClient(authResult: AuthResult, channel: Channel[Message])(implicit request: RequestHeader): Client =
    PhoneClient(authResult, channel, request)

  override def onMessage(msg: Message, client: Client): Unit = {
    client.connectedServer send msg
  }

  override def welcomeMessage(client: Client): Option[Message] = Some(com.mle.play.json.JsonMessages.welcome)
}

case class PhoneClient(connectedServer: PimpSocket, phoneChannel: Channel[JsValue], req: RequestHeader)
