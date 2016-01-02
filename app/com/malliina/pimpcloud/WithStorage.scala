package com.malliina.pimpcloud

import com.malliina.ws.WebSocketBase2

/**
  * @author mle
  */
trait WithStorage[M, C <: com.malliina.play.ws.SocketClient[M]] extends WebSocketBase2[M, C] {
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
