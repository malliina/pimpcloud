package com.mle.ws

import scala.collection.concurrent.TrieMap

/**
 * @author Michael
 */
trait TrieClientStorage extends com.mle.play.ws.WebSocketBase {
  protected val clientsMap = TrieMap.empty[Client, Unit]

  override def clients = clientsMap.keys.toSeq

  override def onDisconnect(client: Client): Unit = {
    clientsMap -= client
  }

  override def onConnect(client: Client): Unit = {
    clientsMap += (client -> (()))
  }
}
