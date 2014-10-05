package com.mle.ws

import com.mle.musicpimp.cloud.PimpSocket
import com.mle.util.Log
import controllers.StreamSocket
import play.api.libs.iteratee.Concurrent.Channel
import play.api.mvc._
import rx.lang.scala.Subject

import scala.collection.concurrent.TrieMap

/**
 * @author Michael
 */
trait ServerSocket extends StreamSocket with Log {
  val subject = Subject[SocketMessage]()
  override type Client = PimpSocket

  val clients = TrieMap.empty[String, Client]

  def openSocketCall: Call

  def wsUrl(implicit request: RequestHeader): String = openSocketCall.webSocketURL(request.secure)

  override def newClient(user: String, channel: Channel[Message])(implicit request: RequestHeader): Client =
    new PimpSocket(channel, user)

  override def onConnect(client: Client): Unit = {
    val clientID = client.id
    clients += clientID -> client
    logEvent(clientID, "connected")
    subject onNext Connected(client)
  }

  override def onDisconnect(client: Client): Unit = {
    val clientID = client.id
    clients -= clientID
    logEvent(clientID, "disconnected")
    subject onNext Disconnected(client)
  }

  def logEvent(id: String, action: String) =
    log info s"MusicPimp client $action: $id. Clients left: ${clients.size}"

  trait SocketMessage

  case class Connected(client: Client) extends SocketMessage

  case class Disconnected(client: Client) extends SocketMessage

}
