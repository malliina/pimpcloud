package com.mle.ws

import com.mle.musicpimp.cloud.PimpSocket
import com.mle.pimpcloud.actors.{ActorStorage, ServersActor}
import com.mle.play.controllers.AuthResult
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.JsValue
import play.api.mvc._
import rx.lang.scala.subjects.BehaviorSubject

/**
 * @author Michael
 */
abstract class ServerSocket(storage: ActorStorage[ServersActor, JsValue, PimpSocket]) extends ServerActorSockets(storage) {
  override type AuthSuccess = AuthResult
  val subject = BehaviorSubject[SocketEvent](Users(Nil))

  def openSocketCall: Call

  override def newClient(user: AuthSuccess, channel: Channel[JsValue])(implicit request: RequestHeader): PimpSocket =
    new PimpSocket(channel, user.user, request, updateRequestList)

  def updateRequestList(): Unit

  override def onConnect(client: PimpSocket): Unit = {
    super.onConnect(client)
    subject onNext Connected(client)
  }

  override def onDisconnect(client: PimpSocket): Unit = {
    super.onDisconnect(client)
    subject onNext Disconnected(client)
  }

  trait SocketEvent

  case class Users(users: Seq[PimpSocket]) extends SocketEvent

  case class Connected(client: PimpSocket) extends SocketEvent

  case class Disconnected(client: PimpSocket) extends SocketEvent
}
