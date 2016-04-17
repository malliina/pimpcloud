package com.malliina.pimpcloud

import com.malliina.play.ws.{SocketClient, WebSocketBase}

trait ClientStorage[M, C <: SocketClient[M]] extends WebSocketBase {

  override type Message = M
  override type Client = C

  def storage: AsyncClientHandler[M, C]

  override def onConnect(client: C): Unit =
    storage.onConnect(client)

  override def onDisconnect(client: C): Unit =
    storage.onDisconnect(client)

//  override def broadcast(message: M): Unit =
//    storage.broadcast(message)
}
