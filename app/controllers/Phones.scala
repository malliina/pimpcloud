package controllers

import java.net.{URLEncoder, URLDecoder}
import java.nio.file.Paths

import com.mle.concurrent.ExecutionContexts.cached
import com.mle.concurrent.FutureOps
import com.mle.musicpimp.audio.Directory
import com.mle.musicpimp.cloud.PimpSocket
import com.mle.musicpimp.cloud.PimpSocket.{json, jsonID}
import com.mle.musicpimp.json.JsonStrings._
import com.mle.pimpcloud.{ErrorMessage, ErrorResponse}
import com.mle.pimpcloud.ws.PhoneSockets
import com.mle.play.ContentRange
import com.mle.play.controllers.{BaseController, BaseSecurity}
import com.mle.play.http.HttpConstants.{AUDIO_MPEG, NO_CACHE}
import play.api.http.ContentTypes
import play.api.libs.MimeTypes
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import rx.lang.scala.Observable

import scala.concurrent.Future
import scala.util.Try

/**
 *
 * @author mle
 */
class Phones(val servers: Servers, val phoneSockets: PhoneSockets) extends Controller with Secured with BaseSecurity with BaseController {
  val DEFAULT_LIMIT = 100
  val BYTES = "bytes"
  val NONE = "none"

  def combineAll[T](obs: List[Observable[T]], f: (T, T) => T): Observable[T] = obs match {
    case Nil => Observable.never
    case h :: t => h.combineLatestWith(combineAll(t, f))(f)
  }

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

  def beam = BodyProxied(BEAM)

  /**
   * Relays track `id` to the client from the target.
   *
   * Sends a message over WebSocket to the target that it should send `id` to this server. This server then forwards the
   * response of the target to the client.
   *
   * @param id id of the requested track
   */
  def track(id: String): EssentialAction = {
    log debug s"Got request of: $id"
    phoneAction(socket => {
      Action.async(req => {
        Phones.path(id).map(path => {
          val name = path.getFileName.toString
          // resolves track metadata from the server so we can set Content-Length
          log debug s"Looking up meta..."
          socket.meta(id).map(track => {
            // proxies request
            val trackSize = track.size
            val rangeTry = ContentRange.fromHeader(req, trackSize)
            val rangeOrAll = rangeTry getOrElse ContentRange.all(trackSize)
            val resultOpt = socket.streamRange(track, rangeOrAll)
            resultOpt.map(result => {
              rangeTry.map(range => {
                result.withHeaders(
                  CONTENT_RANGE -> range.contentRange,
                  CONTENT_LENGTH -> s"${range.contentLength}",
                  CONTENT_TYPE -> MimeTypes.forFileName(name).getOrElse(ContentTypes.BINARY)
                )
              }).getOrElse {
                result.withHeaders(
                  ACCEPT_RANGES -> BYTES,
                  CONTENT_LENGTH -> trackSize.toBytes.toString,
                  CACHE_CONTROL -> NO_CACHE,
                  CONTENT_TYPE -> AUDIO_MPEG,
                  CONTENT_DISPOSITION -> s"""attachment; filename="$name"""")
              }
            }).getOrElse(BadRequest)
          }).recoverAll(_ => notFound(s"ID not found $id"))
        }).getOrElse(Future.successful(badRequest(s"Illegal track ID $id")))
      })
    })
  }

  def notFound(message: String) = NotFound(simpleError(message))

  def badRequest(message: String) = BadRequest(simpleError(message))

  def simpleError(message: String) = ErrorResponse(Seq(ErrorMessage(message)))

  def BodyProxied(cmd: String) =
    ProxiedJsonAction(parse.json)(req => Some(PimpSocket.bodiedJson(cmd, req.body.as[JsObject])))

  private def FolderAction(html: PimpSocket => Future[Directory], json: RequestHeader => JsObject) =
    ProxiedAction((req, socket) => FolderResult(socket, html, json(req))(req))

  private def FolderResult(socket: PimpSocket, html: PimpSocket => Future[Directory], json: => JsObject)(implicit req: RequestHeader) =
    pimpResult(
      html(socket).map(dir => Ok(views.html.index(dir, phoneSockets.wsUrl))),
      proxiedJson(socket, json)
    ).recoverAll(_ => BadGateway)

  private def ProxiedAction[A](parser: BodyParser[A])(f: (Request[A], PimpSocket) => Future[Result]): EssentialAction =
    phoneAction(socket => Action.async(parser)(implicit req => f(req, socket)))

  private def ProxiedAction(f: (Request[AnyContent], PimpSocket) => Future[Result]): EssentialAction =
    ProxiedAction(parse.anyContent)(f)

  private def ProxiedGetAction(cmd: String) = ProxiedJsonAction(json(cmd))

  private def ProxiedJsonAction(message: JsObject): EssentialAction = ProxiedJsonAction(_ => Some(message))

  private def ProxiedJsonAction(f: RequestHeader => Option[JsObject]): EssentialAction =
    ProxiedJsonAction(parse.anyContent)(f)

  private def ProxiedJsonAction[A](parser: BodyParser[A])(f: Request[A] => Option[JsObject]): EssentialAction =
    phoneAction(socket => {
      Action.async(parser)(implicit req => f(req)
        .fold[Future[Result]](Future.successful(BadRequest))(json => {
        proxiedJson(socket, json).recoverAll(t => BadGateway)
      }))
    })

  def proxiedJson(socket: PimpSocket, json: JsObject) = (socket defaultProxy json) map (js => Ok(js))

  def phoneAction(f: PimpSocket => EssentialAction) = LoggedSecureActionAsync(servers.authPhone)(f)

  def fut[T](body: => T) = Future successful body
}

object Phones {
  val invalidCredentials = new NoSuchElementException("Invalid credentials")
  val EncodingUTF8 = "UTF-8"

  def path(id: String) = Try(Paths get decode(id))

  def decode(id: String) = URLDecoder.decode(id, EncodingUTF8)

  def encode(id: String) = URLEncoder.encode(id, EncodingUTF8)
}
