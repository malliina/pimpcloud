package controllers

import java.util.UUID

import akka.actor.ActorSystem
import com.mle.concurrent.ExecutionContexts.cached
import com.mle.concurrent.FutureOps
import com.mle.musicpimp.cloud.PimpServerSocket
import com.mle.musicpimp.json.JsonStrings
import com.mle.musicpimp.json.JsonStrings.{ADDRESS, BODY, CMD, EVENT, ID, REGISTERED, SERVERS}
import com.mle.musicpimp.models.User
import com.mle.pimpcloud.actors.ActorStorage
import com.mle.pimpcloud.ws.StreamData
import com.mle.pimpcloud.{CloudCredentials, PimpAuth}
import com.mle.play.auth.Auth
import com.mle.play.controllers.AuthResult
import com.mle.ws.ServerSocket
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc._
import rx.lang.scala.Observable
import rx.lang.scala.subjects.BehaviorSubject

import scala.concurrent.Future

/**
  * WebSocket for connected MusicPimp servers.
  *
  * Pushes player events sent by servers to any connected phones, and responds to requests.
  *
  * @author Michael
  */
abstract class Servers(actorSystem: ActorSystem) extends ServerSocket(ActorStorage.servers(actorSystem)) {
  // not a secret but avoids unintentional connections
  val serverPassword = "pimp"

  implicit val writer = Writes[PimpServerSocket](o => Json.obj(
    ID -> o.id,
    ADDRESS -> o.headers.remoteAddress
  ))
  val usersJson = storage.users.map(list => Json.obj(EVENT -> SERVERS, BODY -> list))

  val streamSubject = BehaviorSubject[Seq[StreamData]](Nil)
  val uuidsJson: Observable[JsValue] = streamSubject.map(streams => Json.obj(
    EVENT -> JsonStrings.REQUESTS,
    BODY -> streams
  ))

  def updateRequestList() = ongoingTransfers.foreach(ts => streamSubject.onNext(ts.toSeq))

  private def ongoingTransfers = connectedServers.map(_.flatMap(_.fileTransfers.snapshot))

  override def openSocketCall: Call = routes.Servers.openSocket()

  def newID(): String = UUID.randomUUID().toString take 5

  /**
    * The server must authenticate with Basic HTTP authentication. The username must either be an empty string for
    * new clients, or a previously used cloud ID for old clients. The password must be pimp.
    *
    * @param request
    * @return a valid cloud ID, or None if the cloud ID generation failed
    */
  override def authenticateAsync(request: RequestHeader): Future[AuthResult] = {
    Auth.basicCredentials(request).filter(_.password == serverPassword).map(creds => {
      val user = creds.username
      val cloudID: Future[String] =
        if (user.nonEmpty) {
          isConnected(user).flatMap(connected => {
            if (connected) {
              val msg = s"Unable to register client: $user. Another client with that ID is already connected."
              log warn msg
              Future.failed(new NoSuchElementException(msg))
            } else {
              Future.successful(user)
            }
          })
        } else {
          val id = newID()
          isConnected(id).flatMap(connected => {
            if (connected) {
              val msg = s"A collision occurred while generating a random client ID: $id. Unable to register client."
              log error msg
              Future.failed(new NoSuchElementException(msg))
            } else {
              Future.successful(id)
            }
          })
        }
      cloudID map (id => com.mle.play.controllers.AuthResult(id))
    }).getOrElse(Future.failed(new NoSuchElementException))
  }


  override def welcomeMessage(client: PimpServerSocket): Option[JsValue] = {
    Some(Json.obj(CMD -> REGISTERED, BODY -> Json.obj(ID -> client.id)))
  }

  def isConnected(serverID: String): Future[Boolean] = {
    connectedServers.exists(cs => cs.exists(_.id == serverID))
  }

  def connectedServers: Future[Set[PimpServerSocket]] = storage.clients

  override def onMessage(msg: JsValue, client: PimpServerSocket): Boolean = {
    log debug s"Got message: $msg from client: $client"

    val isUnregister = false // (msg \ CMD).validate[String].filter(_ == UNREGISTER).isSuccess
    if (isUnregister) {
      //      identities remove client.id
      false
    } else {
      val clientHandledMessage = client complete msg
      // forwards non-requested events to any connected phones

      // The fact a client refuses to handle a response doesn't mean it's meant for someone else. The response may for
      // example have been ignored by the client because it arrived too late. This logic is thus not solid. The
      // consequence is that clients may receive unsolicited messages occasionally. But they should ignore those anyway,
      // so we accept this failure.
      if (!clientHandledMessage) {
        sendToPhone(msg, client)
      }
      clientHandledMessage
    }
  }

  def sendToPhone(msg: JsValue, client: PimpServerSocket): Unit

  def authPhone(req: RequestHeader): Future[PhoneConnection] =
    connectedServers.flatMap(servers => authPhone(req, servers))

  /**
    * Fails with a [[NoSuchElementException]] if authentication fails.
    *
    * @param req request
    * @return the socket, if auth succeeds
    */
  def authPhone(req: RequestHeader, servers: Set[PimpServerSocket]): Future[PhoneConnection] = {
    // header -> query -> session
    headerAuthAsync(req, servers)
      .recoverWithAll(_ => queryAuth(req, servers))
      .recoverAll(_ => sessionAuth(req, servers).get)
  }

  def headerAuthAsync(req: RequestHeader, servers: Set[PimpServerSocket]): Future[PhoneConnection] = flattenInvalid {
    PimpAuth.cloudCredentials(req).map(creds => validate(creds, servers))
  }

  def queryAuth(req: RequestHeader, servers: Set[PimpServerSocket]): Future[PhoneConnection] = flattenInvalid {
    for {
      s <- req.queryString get JsonStrings.SERVER_KEY
      server <- s.headOption
      creds <- Auth.credentialsFromQuery(req)
    } yield validate(CloudCredentials(server, User(creds.username), creds.password), servers)
  }

  def sessionAuth(req: RequestHeader, servers: Set[PimpServerSocket]): Option[PhoneConnection] = {
    req.session.get(Security.username)
      .flatMap(user => servers.find(_.id == user).map(server => PhoneConnection(User(user), server)))
  }

  def validate(creds: CloudCredentials): Future[PhoneConnection] =
    connectedServers.flatMap(servers => validate(creds, servers))

  /**
    * @param creds
    * @return a socket or a [[Future]] failed with [[NoSuchElementException]] if validation fails
    */
  def validate(creds: CloudCredentials, servers: Set[PimpServerSocket]): Future[PhoneConnection] = flattenInvalid {
    servers.find(_.id == creds.cloudID).map(server => {
      val user = creds.username
      server.authenticate(user.name, creds.password).filter(_ == true).map(_ => PhoneConnection(user, server))
    })
  }

  def flattenInvalid[T](optFut: Option[Future[T]]) =
    optFut getOrElse Future.failed[T](Phones.invalidCredentials)
}

case class Server(request: UUID, socket: PimpServerSocket)

case class PhoneConnection(user: User, server: PimpServerSocket)
