package com.mle.pimpcloud

import com.mle.ws.WebSocketBase2

/**
 * @author mle
 */
trait WithStorage[M, C <: com.mle.play.ws.SocketClient[M]] extends WebSocketBase2[M, C] {
  def storage: ClientHandler[M, C]

  override def onConnect(client: C): Unit = {
    storage.onConnect(client)
  }

  override def onDisconnect(client: C): Unit = {
    storage.onDisconnect(client)
  }

  override def broadcast(message: M): Unit = {
    storage.broadcast(message)
  }
}
