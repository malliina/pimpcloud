package com.mle.ws

import com.mle.musicpimp.cloud.PimpSocket
import com.mle.play.controllers.AuthResult
import com.mle.play.ws.JsonWebSockets
import com.mle.util.Log
import controllers.UsersEvents
import play.api.libs.iteratee.Concurrent.Channel
import play.api.mvc._
import rx.lang.scala.subjects.BehaviorSubject

import scala.collection.concurrent.TrieMap

/**
 * @author Michael
 */
trait ServerSocket extends JsonWebSockets with Log {
  override type AuthSuccess = AuthResult
  override type Client = PimpSocket
  val subject = BehaviorSubject[SocketEvent](Users(Nil))
  val servers = TrieMap.empty[String, Client]

  override def clients: Seq[Client] = servers.values.toSeq

  def openSocketCall: Call

  override def newClient(user: AuthSuccess, channel: Channel[Message])(implicit request: RequestHeader): Client =
    new PimpSocket(channel, user.user, request)

  override def onConnect(client: Client): Unit = {
    val clientID = client.id
    servers += clientID -> client
    logEvent(clientID, "connected")
    subject onNext Connected(client)
  }

  override def onDisconnect(client: Client): Unit = {
    val clientID = client.id
    servers -= clientID
    logEvent(clientID, "disconnected")
    subject onNext Disconnected(client)
  }

  def logEvent(id: String, action: String) =
    log info s"MusicPimp client $action: $id. Clients connected: ${servers.size}"

  trait SocketEvent

  case class Users(users: Seq[Client]) extends SocketEvent

  case class Connected(client: Client) extends SocketEvent

  case class Disconnected(client: Client) extends SocketEvent
}
