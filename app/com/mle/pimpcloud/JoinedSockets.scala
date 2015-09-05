package com.mle.pimpcloud

import com.mle.musicpimp.cloud.PimpSocket
import com.mle.pimpcloud.ws.PhoneClient
import com.mle.play.controllers.AuthResult
import com.mle.ws.TrieClientStorage
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader

/**
 * @author mle
 */
class JoinedSockets {
  val s = new S {
    override def onMessage(msg: JsValue, client: PimpSocket): Boolean = {
      onServerMessage(msg, client)
    }
  }

  val ps = new PS {
    override def onMessage(msg: JsValue, client: PhoneClient): Boolean = {
      onPhoneMessage(msg, client)
    }
  }

  def onServerMessage(msg: JsValue, client: PimpSocket): Boolean = {
    true
  }

  def onPhoneMessage(msg: JsValue, client: PhoneClient): Boolean = {
    true
  }
}

class S extends TrieClientStorage {
  override type AuthSuccess = AuthResult
  override type Message = JsValue
  override type Client = PimpSocket

  override def wsUrl(implicit request: RequestHeader): String = ???

  override def newClient(user: AuthSuccess, channel: Channel[Message])(implicit request: RequestHeader): PimpSocket =
    new PimpSocket(channel, user.user, request, onUpdate = () => ())

  override def broadcast(message: Message): Unit = clients.foreach(_.channel push message)

  override def onMessage(msg: JsValue, client: PimpSocket): Boolean = {
    // send to connected phonesockets
    super.onMessage(msg, client)
  }
}

class PS extends TrieClientStorage {
  override type AuthSuccess = PimpSocket
  override type Message = JsValue
  override type Client = PhoneClient

  override def wsUrl(implicit request: RequestHeader): String = ???

  override def newClient(authResult: AuthSuccess, channel: Channel[Message])(implicit request: RequestHeader): Client =
    PhoneClient(authResult, channel, request)

  override def broadcast(message: Message): Unit = clients.foreach(_.channel push message)

  override def onMessage(msg: JsValue, client: PhoneClient): Boolean = {
    // send to server
    super.onMessage(msg, client)
  }
}