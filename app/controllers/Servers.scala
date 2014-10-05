package controllers

import java.util.UUID

import com.mle.musicpimp.json.JsonStrings.{CMD, EVENT, ID, REGISTERED, UNREGISTER}
import com.mle.pimpcloud.CloudIdentityStore
import com.mle.pimpcloud.db.CloudDatabase
import com.mle.pimpcloud.ws.PhoneSockets
import com.mle.play.auth.Auth
import com.mle.play.controllers.AuthResult
import com.mle.ws.ServerSocket
import play.api.libs.json.Json
import play.api.mvc.{Call, Controller, RequestHeader}

/**
 * @author Michael
 */
object Servers extends Controller with ServerSocket {
  // not a secret but avoids unintentional connections
  val serverPassword = "pimp"
  val identities: CloudIdentityStore = CloudDatabase.default

  override def openSocketCall: Call = routes.Servers.openSocket()

  /**
   * The server must authenticate with Basic HTTP authentication. The username must either be an empty string for
   * new clients, or a previously used cloud ID for old clients. The password must be pimp.
   *
   * @param request
   * @return a valid cloud ID, or None if the cloud ID generation failed
   */
  override def authenticate(implicit request: RequestHeader): Option[AuthResult] = {
    Auth.basicCredentials(request).filter(_.password == serverPassword).flatMap(creds => {
      val user = creds.username
      val cloudID =
        if (user.nonEmpty) {
          if (identities exists user) {
            if (isConnected(user)) {
              log warn s"Unable to register client: $user. Another client with that ID is already connected."
              None
            } else {
              // connects with a previously used ID
              Some(user)
            }
          } else {
            // connects with a user-desired ID
            if (isConnected(user)) {
              log warn s"Client: $user is currently connected but unregistered; this is most likely an anomaly."
              None
            } else {
              identities.trySave(user).fold(alreadyExists => None, id => Some(id))
            }
          }
        } else {
          // no previous ID -> generates a new, random ID
          identities.generateAndSave().fold(ae => {
            log error s"A collision occurred while generating a random client ID. Unable to register client."
            None
          }, id => Some(id))
        }
      cloudID map (id => AuthResult(id))
    })
  }

  def newCloudID = UUID.randomUUID().toString take 5

  override def welcomeMessage2(client: Client): Option[Message] =
    Some(Json.toJson(Map(EVENT -> REGISTERED, ID -> client.id)))

  def isConnected(serverID: String) = clients contains serverID

  override def onMessage(msg: Message, client: Client): Unit = {
    log debug s"Got message: $msg from client: $client"

    val isUnregister = (msg \ CMD).validate[String].filter(_ == UNREGISTER).isSuccess
    if (isUnregister) {
      identities remove client.id
    } else {
      val clientHandledMessage = client complete msg
      // forwards non-requested events to any connected phones

      // The fact a client refuses to handle a response doesn't mean it's meant for someone else. The response may for
      // example have been ignored by the client because it arrived too late. This logic is thus not solid. The
      // consequence is that clients may receive unsolicited messages occasionally. But they should ignore those anyway,
      // so we accept this failure.
      if (!clientHandledMessage) {
        PhoneSockets.clients.filter(_.connectedServer == client).foreach(_.phoneChannel push msg)
      }
    }
  }
}

