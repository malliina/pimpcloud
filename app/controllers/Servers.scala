package controllers

import java.util.UUID

import com.mle.concurrent.ExecutionContexts.cached
import com.mle.concurrent.FutureOps
import com.mle.musicpimp.cloud.PimpSocket
import com.mle.musicpimp.json.JsonStrings
import com.mle.musicpimp.json.JsonStrings.{ADDRESS, BODY, EVENT, ID, REGISTERED, SERVERS}
import com.mle.pimpcloud.ws.{PhoneSockets, StreamData}
import com.mle.pimpcloud.{CloudCredentials, PimpAuth}
import com.mle.play.auth.Auth
import com.mle.play.ws.SyncAuth
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
class Servers extends Controller with ServerSocket with SyncAuth with UsersEvents {
  // not a secret but avoids unintentional connections
  val serverPassword = "pimp"

  implicit val writer = Writes[PimpSocket](o => Json.obj(
    ID -> o.id,
    ADDRESS -> o.headers.remoteAddress
  ))
  val usersJson = users.map(list => Json.obj(EVENT -> SERVERS, BODY -> list))

  val streamSubject = BehaviorSubject[Seq[StreamData]](Nil)
  val uuidsJson: Observable[JsValue] = streamSubject.map(streams => Json.obj(
    EVENT -> JsonStrings.REQUESTS,
    BODY -> streams
  ))

  def updateRequestList() = streamSubject onNext ongoingTransfers

  private def ongoingTransfers = clients.flatMap(_.fileTransfers.snapshot)

  // SIN
  private var phoneSockets: Option[PhoneSockets] = None

  def register(ps: PhoneSockets) = {
    phoneSockets = Option(ps)
  }

  override def openSocketCall: Call = routes.Servers.openSocket()

  def newID(): String = UUID.randomUUID().toString take 5

  /**
   * The server must authenticate with Basic HTTP authentication. The username must either be an empty string for
   * new clients, or a previously used cloud ID for old clients. The password must be pimp.
   *
   * @param request
   * @return a valid cloud ID, or None if the cloud ID generation failed
   */
  override def authenticate(implicit request: RequestHeader): Option[AuthSuccess] = {
    Auth.basicCredentials(request).filter(_.password == serverPassword).flatMap(creds => {
      val user = creds.username
      val cloudID =
        if (user.nonEmpty) {
          if (isConnected(user)) {
            log warn s"Unable to register client: $user. Another client with that ID is already connected."
            None
          } else {
            Some(user)
          }
        } else {
          val id = newID()
          if (isConnected(id)) {
            log error s"A collision occurred while generating a random client ID: $id. Unable to register client."
            None
          } else {
            Some(id)
          }
        }
      cloudID map (id => com.mle.play.controllers.AuthResult(id))
    })
  }

  override def welcomeMessage(client: Client): Option[Message] = {
    Some(PimpSocket.jsonID(REGISTERED, client.id))
  }

  def isConnected(serverID: String) = servers contains serverID

  override def onMessage(msg: Message, client: Client): Boolean = {
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
        phoneSockets.foreach(ps => ps.clients.filter(_.connectedServer == client).foreach(_.channel push msg))
      }
      clientHandledMessage
    }
  }

  def sendToPhone(msg: Message, client: Client): Unit = ()

  def authPhone(req: RequestHeader): Future[PimpSocket] = authPhone(req, servers.toMap)

  /**
   * Fails with a [[NoSuchElementException]] if authentication fails.
   *
   * @param req request
   * @return the socket, if auth succeeds
   */
  def authPhone(req: RequestHeader, connectedServers: Map[String, Client]): Future[PimpSocket] = {
    // header -> query -> session
    headerAuthAsync(req, connectedServers)
      .recoverWithAll(_ => queryAuth(req, connectedServers))
      .recoverAll(_ => sessionAuth(req, connectedServers).get)
  }

  def headerAuthAsync(req: RequestHeader, connectedServers: Map[String, Client]): Future[PimpSocket] = flattenInvalid {
    PimpAuth.cloudCredentials(req).map(validate(_, connectedServers))
  }

  def queryAuth(req: RequestHeader, connectedServers: Map[String, Client]): Future[PimpSocket] = flattenInvalid {
    for {
      s <- req.queryString get JsonStrings.SERVER_KEY
      server <- s.headOption
      creds <- Auth.credentialsFromQuery(req)
    } yield validate(CloudCredentials(server, creds.username, creds.password), connectedServers)
  }

  def sessionAuth(req: RequestHeader, connectedServers: Map[String, Client]): Option[PimpSocket] = {
    req.session.get(Security.username) flatMap servers.get
  }

  def validate(creds: CloudCredentials): Future[PimpSocket] = validate(creds, servers.toMap)

  /**
   * @param creds
   * @return a socket or a [[Future]] failed with [[NoSuchElementException]] if validation fails
   */
  def validate(creds: CloudCredentials, connectedServers: Map[String, Client]): Future[Client] = flattenInvalid {
    connectedServers.get(creds.cloudID)
      .map(server => server.authenticate(creds.username, creds.password).filter(_ == true).map(_ => server))
  }

  def flattenInvalid[T](optFut: Option[Future[T]]) =
    optFut getOrElse Future.failed[T](Phones.invalidCredentials)
}

case class Server(request: UUID, socket: PimpSocket)
