package com.malliina.ws

import akka.stream.Materializer
import akka.stream.scaladsl.SourceQueue
import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.play.http.AuthResult
import play.api.libs.json.JsValue
import play.api.mvc._
import rx.lang.scala.subjects.BehaviorSubject

abstract class ServerSocket(val storage: RxStmStorage[PimpServerSocket], val mat: Materializer)
  extends ServerActorSockets {

  override type Client = PimpServerSocket
  override type AuthSuccess = AuthResult
  val subject = BehaviorSubject[SocketEvent](Users(Nil))

  def openSocketCall: Call

  override def newClient(user: AuthSuccess, channel: SourceQueue[JsValue])(implicit request: RequestHeader): PimpServerSocket =
    new PimpServerSocket(channel, user.user, request, mat, updateRequestList)

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
