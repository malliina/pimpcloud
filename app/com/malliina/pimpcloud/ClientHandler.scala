package com.malliina.pimpcloud

import scala.concurrent.Future

trait ClientHandler[M, C] {
  def onConnect(c: C)

  def onDisconnect(c: C)

  def broadcast(m: M)

  def clients: Future[Set[C]]
}
