package com.malliina.pimpcloud.ws

import com.malliina.play.ws.WebSocketBase
import com.malliina.ws.SocketStorage

trait StorageSocket {
  self: WebSocketBase =>
  def storage: SocketStorage[Client]

  override def clients: Seq[Client] = storage.clients

  override def onConnect(client: Client): Unit = storage.onConnect(client)

  override def onDisconnect(client: Client): Unit = storage.onDisconnect(client)
}
