package com.mle.pimpcloud

import akka.actor.ActorSystem
import com.mle.musicpimp.cloud.PimpServerSocket
import com.mle.pimpcloud.actors.PhonesActor
import com.mle.pimpcloud.ws.PhoneSockets
import controllers.{PhoneConnection, Servers}
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader

import scala.concurrent.Future

/**
 * @author mle
 */
class JoinedSockets(actorSystem: ActorSystem) {
  val servers = new Servers(actorSystem) {
    override def sendToPhone(msg: JsValue, client: PimpServerSocket): Unit =
      onServerMessage(msg, client)
  }

  val phones = new PhoneSockets(actorSystem) {
    override def authenticatePhone(req: RequestHeader): Future[PhoneConnection] =
      authPhone(req)
  }

  def onServerMessage(msg: JsValue, server: PimpServerSocket) = {
    phones.storage.actor ! PhonesActor.MessageFromServer(msg, server)
  }

  def authPhone(req: RequestHeader): Future[PhoneConnection] = {
    servers.authPhone(req)
  }
}

object JoinedSockets {
  def joined(actorSystem: ActorSystem): (Servers, PhoneSockets) = {
    val joined = new JoinedSockets(actorSystem)
    (joined.servers, joined.phones)
  }
}
