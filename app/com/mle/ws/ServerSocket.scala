package com.mle.ws

import com.mle.musicpimp.cloud.{PimpServerSocket, PimpServerSocket$}
import com.mle.pimpcloud.actors.{ActorStorage, ServersActor}
import com.mle.play.controllers.AuthResult
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.JsValue
import play.api.mvc._
import rx.lang.scala.subjects.BehaviorSubject

/**
 * @author Michael
 */
abstract class ServerSocket(storage: ActorStorage[ServersActor, JsValue, PimpServerSocket]) extends ServerActorSockets(storage) {
  override type AuthSuccess = AuthResult
  val subject = BehaviorSubject[SocketEvent](Users(Nil))

  def openSocketCall: Call

  override def newClient(user: AuthSuccess, channel: Channel[JsValue])(implicit request: RequestHeader): PimpServerSocket =
    new PimpServerSocket(channel, user.user, request, updateRequestList)

  def updateRequestList(): Unit

  override def onConnect(client: PimpServerSocket): Unit = {
    super.onConnect(client)
    subject onNext Connected(client)
  }

  override def onDisconnect(client: PimpServerSocket): Unit = {
    super.onDisconnect(client)
    subject onNext Disconnected(client)
  }

  trait SocketEvent

  case class Users(users: Seq[PimpServerSocket]) extends SocketEvent

  case class Connected(client: PimpServerSocket) extends SocketEvent

  case class Disconnected(client: PimpServerSocket) extends SocketEvent
}
