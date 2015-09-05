package com.mle.pimpcloud

import com.mle.musicpimp.cloud.PimpSocket
import com.mle.pimpcloud.ws.PhoneSockets
import controllers.Servers
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader

import scala.concurrent.Future

/**
 * @author mle
 */
class JoinedSockets {
  val servers = new Servers {
    override def sendToPhone(msg: JsValue, client: PimpSocket): Unit =
      onServerMessage(msg, client)
  }

  val phones = new PhoneSockets {
    override def authenticatePhone(req: RequestHeader): Future[PimpSocket] =
      authPhone(req)
  }

  def onServerMessage(msg: JsValue, client: PimpSocket) = {
    phones.clients.filter(_.connectedServer == client).foreach(_.channel push msg)
  }

  def authPhone(req: RequestHeader): Future[PimpSocket] = {
    servers.authPhone(req)
  }
}

object JoinedSockets {
  def joined: (Servers, PhoneSockets) = {
    val joined = new JoinedSockets
    (joined.servers, joined.phones)
  }
}
