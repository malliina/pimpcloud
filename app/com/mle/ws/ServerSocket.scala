package com.mle.ws

import com.mle.musicpimp.cloud.PimpSocket
import com.mle.play.ws.JsonWebSockets
import com.mle.util.Log
import play.api.libs.iteratee.Concurrent.Channel
import play.api.mvc._
import rx.lang.scala.Subject

import scala.collection.concurrent.TrieMap

/**
 * @author Michael
 */
trait ServerSocket extends JsonWebSockets with Log {
  override type AuthSuccess = com.mle.play.controllers.AuthResult
  val subject = Subject[SocketMessage]()
  override type Client = PimpSocket

  val servers = TrieMap.empty[String, Client]

  override def clients: Seq[Client] = servers.values.toSeq

  def openSocketCall: Call

  override def newClient(user: AuthSuccess, channel: Channel[Message])(implicit request: RequestHeader): Client =
    new PimpSocket(channel, user.user)

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

  trait SocketMessage

  case class Connected(client: Client) extends SocketMessage

  case class Disconnected(client: Client) extends SocketMessage

}
