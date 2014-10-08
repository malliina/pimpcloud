package controllers

import java.util.UUID

import com.mle.musicpimp.json.JsonStrings.{EVENT, ID, REGISTERED}
import com.mle.pimpcloud.ws.PhoneSockets
import com.mle.play.auth.Auth
import com.mle.play.controllers.AuthResult
import com.mle.play.ws.SyncAuth
import com.mle.ws.ServerSocket
import play.api.libs.json.Json
import play.api.mvc.{Call, Controller, RequestHeader}

/**
 * @author Michael
 */
object Servers extends Controller with ServerSocket with SyncAuth {
  // not a secret but avoids unintentional connections
  val serverPassword = "pimp"
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
//    Auth.basicCredentials(request).filter(_.password == serverPassword)
//      .map(_.username).map(user => if (user.nonEmpty) user else newID())
//      .filterNot(isConnected)

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

  override def welcomeMessage(client: Client): Option[Message] =
    Some(Json.toJson(Map(EVENT -> REGISTERED, ID -> client.id)))

  def isConnected(serverID: String) = servers contains serverID

  override def onMessage(msg: Message, client: Client): Unit = {
    log debug s"Got message: $msg from client: $client"

    val isUnregister = false // (msg \ CMD).validate[String].filter(_ == UNREGISTER).isSuccess
    if (isUnregister) {
      //      identities remove client.id
    } else {
      val clientHandledMessage = client complete msg
      // forwards non-requested events to any connected phones

      // The fact a client refuses to handle a response doesn't mean it's meant for someone else. The response may for
      // example have been ignored by the client because it arrived too late. This logic is thus not solid. The
      // consequence is that clients may receive unsolicited messages occasionally. But they should ignore those anyway,
      // so we accept this failure.
      if (!clientHandledMessage) {
        PhoneSockets.clients.filter(_.connectedServer == client).foreach(_.channel push msg)
      }
    }
  }
}

