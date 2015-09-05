package com.mle.pimpcloud.actors

import akka.actor.ActorLogging
import com.mle.musicpimp.cloud.PimpSocket
import play.api.libs.json.JsValue

/**
 * Manager of server websockets.
 *
 * @author mle
 */
class ServersActor extends ItemsActor[PimpSocket] with ActorLogging {
  override def receive: Receive = {
    case ServersActor.Connect(client) =>
      clients += client
      logEvent(client.id, "connected")
      sender() ! ServersActor.Clients(clients)
    case ServersActor.Disconnect(client) =>
      clients -= client
      logEvent(client.id, "disconnected")
      sender() ! ServersActor.Clients(clients)
    case ServersActor.Message(json, recipient) =>
      clients.find(_ == recipient).foreach(_.channel push json)
    case ServersActor.Broadcast(json) =>
      clients.foreach(_.channel push json)
    case ServersActor.GetClients =>
      sender() ! ServersActor.Clients(clients)
  }

  def logEvent(id: String, action: String) =
    log info s"MusicPimp client $action: $id. Clients connected: ${clients.size}"
}

object ServersActor extends MessagesBase[JsValue, PimpSocket]
