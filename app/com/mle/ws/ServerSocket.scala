package com.mle.ws

import com.mle.musicpimp.cloud.PimpSocket
import controllers.StreamSocket
import play.api.libs.iteratee.Concurrent.Channel
import play.api.mvc._

import scala.collection.concurrent.TrieMap

/**
 * @author Michael
 */
trait ServerSocket extends StreamSocket {
  override type Client = PimpSocket

  val clients = TrieMap.empty[String, Client]

  def openSocketCall: Call

  def wsUrl(implicit request: RequestHeader): String = openSocketCall.webSocketURL(request.secure)

  override def newClient(user: String, channel: Channel[Message])(implicit request: RequestHeader): Client =
    new PimpSocket(channel, user)

  override def onConnect(client: Client): Unit = clients += client.id -> client

  override def onDisconnect(client: Client): Unit = clients -= client.id
}
