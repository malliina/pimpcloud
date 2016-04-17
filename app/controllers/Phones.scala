package controllers

import java.net.{URLDecoder, URLEncoder}
import java.nio.file.Paths

import akka.stream.Materializer
import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.audio.Directory
import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.models.PlaylistID
import com.malliina.pimpcloud.ws.PhoneSockets
import com.malliina.pimpcloud.{ErrorMessage, ErrorResponse}
import com.malliina.play.ContentRange
import com.malliina.play.controllers.{BaseController, BaseSecurity}
import com.malliina.play.http.HttpConstants.{AUDIO_MPEG, NO_CACHE}
import controllers.Phones.log
import play.api.Logger
import play.api.http.ContentTypes
import play.api.libs.MimeTypes
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc._

import scala.concurrent.Future
import scala.util.Try

class Phones(val servers: Servers, val phoneSockets: PhoneSockets, val mat: Materializer)
  extends Controller
    with Secured
    with BaseSecurity
    with BaseController {

  def ping = proxiedGetAction(PING)

  def pingAuth = proxiedAction((req, socket) => socket.server.pingAuth.map(v => NoCache(Ok(Json toJson v))))

  def rootFolder = folderAction(_.rootFolder, _ => (ROOT_FOLDER, Json.obj()))

  def folder(id: String) = folderAction(_.folder(id), req => (FOLDER, PimpServerSocket.idBody(id)))

  def status = proxiedGetAction(STATUS)

  def search = proxiedAction((req, socket) => {
    def query(key: String) = (req getQueryString key) map (_.trim) filter (_.nonEmpty)
    val termFromQuery = query(TERM)
    val limit = query(LIMIT).filter(i => Try(i.toInt).isSuccess).map(_.toInt) getOrElse Phones.DefaultSearchLimit
    termFromQuery.fold[Future[Result]](Future.successful(BadRequest))(term => {
      folderResult(socket,
        _.search(term, limit).map(tracks => Directory(Nil, tracks)),
        (SEARCH, PimpServerSocket.body(TERM -> term, LIMIT -> limit))
      )(req)
    })
  })

  def alarms = proxiedGetAction(ALARMS)

  def editAlarm = bodyProxied(ALARMS_EDIT)

  def newAlarm = bodyProxied(ALARMS_ADD)

  def beam = bodyProxied(BEAM)

  /**
    * Relays track `id` to the client from the target.
    *
    * Sends a message over WebSocket to the target that it should send `id` to this server. This server then forwards the
    * response of the target to the client.
    *
    * @param id id of the requested track
    */
  def track(id: String): EssentialAction = {
    phoneAction(conn => {
      val socket = conn.server
      Action.async(req => {
        log info s"Serving $id"
        Phones.path(id).map(path => {
          val name = path.getFileName.toString
          // resolves track metadata from the server so we can set Content-Length
          log debug s"Looking up meta..."
          socket.meta(id).flatMap(track => {
            // proxies request
            val trackSize = track.size
            val rangeTry = ContentRange.fromHeader(req, trackSize)
            val rangeOrAll = rangeTry getOrElse ContentRange.all(trackSize)
            val resultFuture = socket.streamRange(track, rangeOrAll)
            resultFuture map { resultOpt =>
              resultOpt.map(result => {
                rangeTry.map(range => {
                  result.withHeaders(
                    CONTENT_RANGE -> range.contentRange,
                    CONTENT_LENGTH -> s"${range.contentLength}",
                    CONTENT_TYPE -> MimeTypes.forFileName(name).getOrElse(ContentTypes.BINARY)
                  )
                }).getOrElse {
                  result.withHeaders(
                    ACCEPT_RANGES -> Phones.BYTES,
                    CONTENT_LENGTH -> trackSize.toBytes.toString,
                    CACHE_CONTROL -> NO_CACHE,
                    CONTENT_TYPE -> AUDIO_MPEG,
                    CONTENT_DISPOSITION -> s"""attachment; filename="$name"""")
                }
              }).getOrElse(BadRequest)
            } recoverAll { err =>
              log.error(s"Cannot compute result", err)
              serverError(s"The server failed")
            }
          }).recoverAll(_ => notFound(s"ID not found $id"))
        }).getOrElse(Future.successful(badRequest(s"Illegal track ID $id")))
      })
    })
  }

  def playlists = proxiedGetAction(PlaylistsGet)

  def playlist(id: PlaylistID) = playlistAction(PlaylistGet, id)

  def savePlaylist = bodyProxied(PlaylistSave)

  def deletePlaylist(id: PlaylistID) = playlistAction(PlaylistDelete, id)

  private def playlistAction(cmd: String, id: PlaylistID) = {
    proxiedJsonAction(cmd, _ => playlistIdJson(id))
  }

  private def playlistIdJson(id: PlaylistID) = Option(Json.obj(ID -> id.id))

  /**
    * Sends the request body as JSON to the server this phone is connected to, and responds with the JSON the server
    * returned.
    *
    * The payload to the connected server will look like: { "cmd": "cmd_here", "body": "request_json_body_here" }
    *
    * @param cmd command to server
    * @return an action that responds as JSON with whatever the connected server returned in its `body` field
    */
  def bodyProxied(cmd: String) = {
    customProxied(cmd, req => req.body.asOpt[JsObject])
  }

  protected def customProxied(cmd: String, body: Request[JsValue] => Option[JsObject]) = {
    proxiedParsedJsonAction(parse.json)(cmd, body)
  }

  private def folderAction(html: PimpServerSocket => Future[Directory],
                           json: RequestHeader => (String, JsObject)) =
    proxiedAction((req, socket) => folderResult(socket, html, json(req))(req))

  private def folderResult(socket: PhoneConnection,
                           html: PimpServerSocket => Future[Directory],
                           json: => (String, JsObject))(implicit req: RequestHeader) =
    pimpResult(
      html(socket.server).map(dir => Ok(views.html.index(dir, phoneSockets.wsUrl))),
      proxiedJson(json._1, json._2, socket)
    ).recoverAll(_ => BadGateway)

  private def proxiedAction(f: (Request[AnyContent], PhoneConnection) => Future[Result]): EssentialAction =
    proxiedParsedAction(parse.anyContent)(f)

  private def proxiedParsedAction[A](parser: BodyParser[A])(f: (Request[A], PhoneConnection) => Future[Result]): EssentialAction =
    phoneAction(socket => Action.async(parser)(implicit req => f(req, socket)))

  private def proxiedGetAction(cmd: String) = proxiedJsonMessageAction(cmd)

  private def proxiedJsonMessageAction(cmd: String): EssentialAction = {
    proxiedJsonAction(cmd, _ => Some(Json.obj()))
  }

  private def proxiedJsonAction(cmd: String, f: RequestHeader => Option[JsObject]): EssentialAction =
    proxiedParsedJsonAction(parse.anyContent)(cmd, f)

  private def proxiedParsedJsonAction[A](parser: BodyParser[A])(cmd: String, f: Request[A] => Option[JsObject]): EssentialAction = {
    phoneAction(socket => {
      Action.async(parser)(req => {
        f(req)
          .map(json => proxiedJson(cmd, json, socket).recoverAll(t => BadGateway))
          .getOrElse(fut(BadRequest))
      })
    })
  }

  def proxiedJson(cmd: String, body: JsValue, conn: PhoneConnection) = {
    conn.server.defaultProxy(conn.user, cmd, body) map (js => Ok(js))
  }

  def phoneAction(f: PhoneConnection => EssentialAction) = LoggedSecureActionAsync(servers.authPhone)(f)

  def fut[T](body: => T) = Future successful body

  def notFound(message: String) = NotFound(simpleError(message))

  def badRequest(message: String) = BadRequest(simpleError(message))

  def serverError(message: String) = InternalServerError(simpleError(message))

  def simpleError(message: String) = ErrorResponse(Seq(ErrorMessage(message)))
}

object Phones {
  val log = Logger(getClass)

  val DefaultSearchLimit = 100
  val BYTES = "bytes"
  val NONE = "none"
  val EncodingUTF8 = "UTF-8"

  val invalidCredentials = new NoSuchElementException("Invalid credentials")

  def path(id: String) = Try(Paths get decode(id))

  def decode(id: String) = URLDecoder.decode(id, EncodingUTF8)

  def encode(id: String) = URLEncoder.encode(id, EncodingUTF8)
}
