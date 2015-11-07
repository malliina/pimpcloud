package com.mle.pimpcloud.actors

import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.mle.musicpimp.cloud.PimpServerSocket
import com.mle.pimpcloud.ClientHandler
import com.mle.pimpcloud.ws.PhoneClient
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsValue
import rx.lang.scala.subjects.BehaviorSubject

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.reflect.ClassTag

object ActorStorage {
  def phones(actorSystem: ActorSystem) =
    new ActorStorage[PhonesActor, JsValue, PhoneClient](actorSystem, PhonesActor)

  def servers(actorSystem: ActorSystem) =
    new ActorStorage[ServersActor, JsValue, PimpServerSocket](actorSystem, ServersActor)
}

/**
 * @author mle
 */
class ActorStorage[A <: Actor : ClassTag, M, C](actorSystem: ActorSystem, val messages: MessagesBase[M, C])
  extends ClientHandler[M, C] {

  // Observable of the connected clients
  val users = BehaviorSubject[Set[C]](Set.empty[C])
  implicit val timeout = Timeout(5.seconds)

  val actor = actorSystem.actorOf(Props[A])

  def updateClients(cs: Set[C]) = users.onNext(cs)

  def onConnect(client: C): Unit = {
    sendAndUpdate(messages.Connect(client))
  }

  def onDisconnect(client: C): Unit = {
    sendAndUpdate(messages.Disconnect(client))
  }

  def broadcast(message: M): Unit = {
    actor ! messages.Broadcast(message)
  }

  def clients: Future[Set[C]] = (actor ? messages.GetClients).mapTo[messages.Clients].map(_.clients)

  private def sendAndUpdate[T](msg: T): Unit = {
    val clients = (actor ? msg).mapTo[messages.Clients]
    clients.map(cs => updateClients(cs.clients))
  }
}
