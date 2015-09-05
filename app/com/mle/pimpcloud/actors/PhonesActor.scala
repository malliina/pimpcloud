package com.mle.pimpcloud.actors

import com.mle.musicpimp.cloud.PimpSocket
import com.mle.pimpcloud.ws.PhoneClient
import play.api.libs.json.JsValue

/**
 * Manager of phone websockets.
 *
 * @author mle
 */
class PhonesActor extends ItemsActor[PhoneClient] {
  override def receive: Receive = {
    case PhonesActor.Connect(client) =>
      clients += client
      sender() ! clients
    case PhonesActor.Disconnect(client) =>
      clients -= client
      sender() ! clients
    case PhonesActor.Message(message, recipient) =>
      clients.find(_ == recipient).foreach(_.channel push message)
    case PhonesActor.MessageFromServer(message, server) =>
      clients.filter(_.connectedServer == server).foreach(_.channel push message)
    case PhonesActor.Broadcast(message) =>
      clients.foreach(_.channel push message)
    case PhonesActor.GetClients =>
      sender() ! clients
  }
}

object PhonesActor extends MessagesBase[JsValue, PhoneClient] {

  case class MessageFromServer(message: JsValue, server: PimpSocket)

}
