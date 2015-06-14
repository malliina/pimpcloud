package com.mle.ws

//import com.mle.pimpcloud.ws.PhoneSockets.Client

import scala.collection.concurrent.TrieMap

/**
 * @author Michael
 */
trait TrieClientStorage extends com.mle.play.ws.WebSocketBase {
  protected val clientsMap = TrieMap.empty[Client, Unit]

  override def clients = clientsMap.keys.toSeq

  override def onDisconnect(client: Client): Unit = {
    clientsMap -= client
//    if (clients.size == 0) {
//      onNoClients()
//    }
  }

  override def onConnect(client: Client): Unit = {
    clientsMap += (client -> (()))
//    if (clients.size == 1) {
//      onFirstClient()
//    }
  }

//  def onFirstClient(): Unit = ()
//
//  def onNoClients(): Unit = ()
}
