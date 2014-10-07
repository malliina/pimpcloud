package controllers

import java.net.URLDecoder
import java.nio.file.Paths
import java.util.UUID

import com.mle.concurrent.FutureImplicits.RichFuture
import com.mle.musicpimp.audio.Directory
import com.mle.musicpimp.cloud.PimpSocket
import com.mle.musicpimp.cloud.PimpSocket.{json, jsonID}
import com.mle.musicpimp.json.JsonStrings.{ALARMS, ALARMS_ADD, ALARMS_EDIT, FOLDER, LIMIT, PING, REQUEST_ID, ROOT_FOLDER, SEARCH, SERVER_KEY, STATUS, TERM, TRACK}
import com.mle.pimpcloud.{CloudCredentials, PimpAuth}
import com.mle.play.auth.Auth
import com.mle.play.concurrent.ExecutionContexts.synchronousIO
import com.mle.play.controllers.{BaseController, BaseSecurity}
import com.mle.play.http.HttpConstants.{AUDIO_MPEG, NO_CACHE}
import com.mle.play.streams.StreamParsers
import com.mle.ws.{IterateeStore, JsonFutureSocket}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._

import scala.concurrent.Future
import scala.util.Try

/**
 *
 * @author mle
 */
object Phones extends Controller with Secured with BaseSecurity with BaseController {
  val DEFAULT_LIMIT = 100
  /**
   * For each incoming request:
   *
   * 1) Assign an ID to the request
   * 2) Open a channel (or create a promise) onto which we push the eventual response
   * 3) Forward the request along with its ID to the destination server
   * 4) The destination server tags its response with the request ID
   * 5) Read the request ID from the response and push the response to the channel (or complete the promise)
   * 6) EOF and close the channel; this completes the request-response cycle
   */
  //  val byteRequests = new RequestStore[Array[Byte]]()
  val fileUploads = new IterateeStore[Array[Byte]]()

  def ping = ProxiedGetAction(PING)

  def pingAuth = ProxiedAction((req, socket) => socket.pingAuth.map(v => NoCache(Ok(Json toJson v))))

  def rootFolder = FolderAction(_.rootFolder, _ => json(ROOT_FOLDER))

  def folder(id: String) = FolderAction(_.folder(id), req => jsonID(FOLDER, id))

  def status = ProxiedGetAction(STATUS)

  def search = ProxiedAction((req, socket) => {
    def query(key: String) = (req getQueryString key) map (_.trim) filter (_.nonEmpty)
    val termFromQuery = query(TERM)
    val limit = query(LIMIT).filter(i => Try(i.toInt).isSuccess).map(_.toInt) getOrElse DEFAULT_LIMIT
    termFromQuery.fold[Future[Result]](Future.successful(BadRequest))(term => {
      FolderResult(socket,
        _.search(term, limit).map(tracks => Directory(Nil, tracks)),
        json(SEARCH, TERM -> term, LIMIT -> limit)
      )(req)
    })
  })

  def alarms = ProxiedGetAction(ALARMS)

  def editAlarm = BodyProxied(ALARMS_EDIT)

  def newAlarm = BodyProxied(ALARMS_ADD)

  def BodyProxied(cmd: String) =
    ProxiedJsonAction(parse.json)(req => Some(PimpSocket.bodiedJson(cmd, req.body.as[JsObject])))

  private def FolderAction(html: PimpSocket => Future[Directory], json: RequestHeader => JsObject) =
    ProxiedAction((req, socket) => FolderResult(socket, html, json(req))(req))

  private def FolderResult(socket: PimpSocket, html: PimpSocket => Future[Directory], json: => JsObject)(implicit req: RequestHeader) =
    pimpResult(
      html(socket).map(dir => Ok(views.html.index(dir))),
      proxiedJson(socket, json)
    ).recoverAll(t => BadGateway)

  private def ProxiedAction[A](parser: BodyParser[A])(f: (Request[A], PimpSocket) => Future[Result]): EssentialAction =
    PhoneAction(socket => Action.async(parser)(implicit req => f(req, socket)))

  private def ProxiedAction(f: (Request[AnyContent], PimpSocket) => Future[Result]): EssentialAction =
    ProxiedAction(parse.anyContent)(f)

  private def ProxiedGetAction(cmd: String) = ProxiedJsonAction(json(cmd))

  private def ProxiedJsonAction(message: JsObject): EssentialAction = ProxiedJsonAction(_ => Some(message))

  private def ProxiedJsonAction(f: RequestHeader => Option[JsObject]): EssentialAction =
    ProxiedJsonAction(parse.anyContent)(f)

  private def ProxiedJsonAction[A](parser: BodyParser[A])(f: Request[A] => Option[JsObject]): EssentialAction =
    PhoneAction(socket => {
      Action.async(parser)(implicit req => f(req)
        .fold[Future[Result]](Future.successful(BadRequest))(json => {
        proxiedJson(socket, json).recoverAll(t => BadGateway)
      }))
    })

  def proxiedJson(socket: PimpSocket, json: JsObject) = (socket defaultProxy json) map (js => Ok(js))

  /**
   * Relays track `id` to the client from the target.
   *
   * Sends a message over WebSocket to the target that it should send `id` to this server. This server then forwards the
   * response of the target to the client.
   *
   * @param id id of the requested track
   */
  def track(id: String) = sendFile(id)

  val BYTES = "bytes"
  val NONE = "none"

  def download(id: String) = sendFile(id, _.withHeaders(ACCEPT_RANGES -> NONE))

  /**
   * Sends a request to a connected server on behalf of a connected phone. Initiated when a phone makes a request to
   * this server. The response of the remote server is relayed back to the phone.
   */
  def sendFile(id: String, f: Result => Result = r => r): EssentialAction = {
    val name = (Paths get decode(id)).getFileName.toString
    val message = jsonID(TRACK, id)
    PhoneAction(socket => {
      Action.async(req => {
        socket.meta(id).map(track => {
          val enumeratorOpt = fileUploads.send(message, socket.channel)
          enumeratorOpt.fold[Result](BadRequest)(enumerator => {
            val result = (Ok feed enumerator).withHeaders(
              CONTENT_LENGTH -> track.size.toBytes.toString,
              CACHE_CONTROL -> NO_CACHE,
              CONTENT_TYPE -> AUDIO_MPEG,
              CONTENT_DISPOSITION -> s"""attachment; filename="$name"""")
            f(result)
          })
        }).recoverAll(_ => NotFound)
      })
    })
  }

  def decode(id: String) = URLDecoder.decode(id, "UTF-8")

  def receiveUpload = ServerAction(server => {
    val requestID = server.request
    fileUploads.remove(requestID).fold[EssentialAction](Action(NotFound))(iteratee => {
      log info s"Streaming file. Request: $requestID."
      Action(StreamParsers.multiPartBodyParser(iteratee))(httpRequest => {
        val files = httpRequest.body.files
        files.foreach(file => {
          log info s"File streaming complete. Request: $requestID."
        })
        Ok
      })
    })
  })

  //  def receiveUpload2 = ServerAction(server => {
  //    val requestID = server.request
  //    byteRequests.remove(requestID).fold[EssentialAction](Action(NotFound))(channel => {
  //      log info s"Streaming response to: $requestID."
  //      Action(StreamParsers.multiPartByteStreaming(channel))(httpRequest => {
  //        val files = httpRequest.body.files
  //        files.foreach(file => {
  //          log info s"File streaming complete. Size: ${file.ref} bytes. Request: $requestID."
  //        })
  //        channel.eofAndEnd()
  //        Ok
  //      })
  //    })
  //  })

  def PhoneAction(f: PimpSocket => EssentialAction) = LoggedSecureActionAsync(authPhone)(f)

  def ServerAction(f: Server => EssentialAction) = LoggedSecureAction(authServer)(f)

  /**
   * Fails with a [[NoSuchElementException]] if authentication fails.
   *
   * @param req request
   * @return the socket, if auth succeeds
   */
  def authPhone(req: RequestHeader): Future[PimpSocket] = {
    // header -> query -> session
    headerAuthAsync(req).recoverWith {
      case t: Throwable => queryAuth(req)
    }.recoverAll(_ => sessionAuth(req).get)
  }

  def sessionAuth(req: RequestHeader) = {
    authenticateFromSession(req) flatMap Servers.servers.get
  }

  def queryAuth(req: RequestHeader) = flattenInvalid {
    for {
      s <- req.queryString get SERVER_KEY
      server <- s.headOption
      creds <- Auth.credentialsFromQuery(req)
    } yield validate(CloudCredentials(server, creds.username, creds.password))
  }

  def headerAuthAsync(req: RequestHeader) = flattenInvalid {
    PimpAuth.cloudCredentials(req).map(validate)
  }

  /**
   *
   * @param creds
   * @return a socket or a [[Future]] failed with [[NoSuchElementException]] if validation fails
   */
  def validate(creds: CloudCredentials): Future[Servers.Client] = flattenInvalid {
    Servers.servers.get(creds.cloudID)
      .map(c => c.authenticate(creds.username, creds.password).filter(_ == true).map(_ => c))
  }

  def flattenInvalid[T](optFut: Option[Future[T]]) =
    optFut getOrElse Future.failed[T](invalidCredentials)

  def authServer(req: RequestHeader): Option[Server] = {
    for {
      requestID <- req.headers get REQUEST_ID
      uuid <- JsonFutureSocket.tryParseUUID(requestID) if fileUploads.exists(uuid)
    } yield Server(uuid)
  }

  case class Server(request: UUID)

  def fut[T](body: => T) = Future successful body

  val invalidCredentials = new NoSuchElementException("Invalid credentials")
}
