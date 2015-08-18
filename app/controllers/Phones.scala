package controllers

import java.net.URLDecoder
import java.nio.file.Paths
import java.util.UUID

import com.mle.concurrent.FutureOps
import com.mle.musicpimp.audio.Directory
import com.mle.musicpimp.cloud.PimpSocket
import com.mle.musicpimp.cloud.PimpSocket.{json, jsonID}
import com.mle.musicpimp.json.JsonStrings._
import com.mle.pimpcloud.ws.StreamData
import com.mle.pimpcloud.{CloudCredentials, PimpAuth}
import com.mle.play.ContentRange
import com.mle.play.auth.Auth
import com.mle.play.concurrent.ExecutionContexts.synchronousIO
import com.mle.play.controllers.{BaseController, BaseSecurity}
import com.mle.play.http.HttpConstants.{AUDIO_MPEG, NO_CACHE}
import com.mle.play.streams.StreamParsers
import com.mle.ws.JsonFutureSocket
import play.api.http.ContentTypes
import play.api.libs.MimeTypes
import play.api.libs.iteratee.{Concurrent, Iteratee}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc._
import rx.lang.scala.Observable
import rx.lang.scala.subjects.BehaviorSubject

import scala.concurrent.Future
import scala.util.Try

/**
 *
 * @author mle
 */
object Phones extends Controller with Secured with BaseSecurity with BaseController {
  val DEFAULT_LIMIT = 100
  val BYTES = "bytes"
  val NONE = "none"
  val subject = BehaviorSubject[Seq[StreamData]](Nil)
  val uuidsJson: Observable[JsValue] = subject.map(streams => Json.obj(
    EVENT -> REQUESTS,
    BODY -> streams
  ))

  def updateRequestList() = subject onNext ongoingTransfers

  def ongoingTransfers = Servers.clients.flatMap(_.fileTransfers.snapshot)

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
   * @param id track ID
   */
  def download(id: String) = track(id)

  /**
   * Relays track `id` to the client from the target.
   *
   * Sends a message over WebSocket to the target that it should send `id` to this server. This server then forwards the
   * response of the target to the client.
   *
   * @param id id of the requested track
   */
  def track(id: String) = sendFile(id)

  def sendFile(id: String): EssentialAction = {
    log debug s"Got request of: $id"
    val name = (Paths get decode(id)).getFileName.toString
    PhoneAction(socket => {
      Action.async(req => {
        // resolves track metadata from the server so we can set Content-Length
        log debug s"Looking up meta..."
        socket.meta(id).map(track => {
          // proxies request
          val trackSize = track.size
          val rangeTry = ContentRange.fromHeader(req, trackSize)
          val enumeratorOpt = socket.stream(track, rangeTry getOrElse ContentRange.all(trackSize))
          enumeratorOpt.map(enumerator => {
            rangeTry.map(range => {
              (PartialContent feed enumerator).withHeaders(
                CONTENT_RANGE -> range.contentRange,
                CONTENT_LENGTH -> s"${range.contentLength}",
                CONTENT_TYPE -> MimeTypes.forFileName(name).getOrElse(ContentTypes.BINARY)
              )
            }).getOrElse {
              (Ok feed enumerator).withHeaders(
                ACCEPT_RANGES -> BYTES,
                CONTENT_LENGTH -> trackSize.toBytes.toString,
                CACHE_CONTROL -> NO_CACHE,
                CONTENT_TYPE -> AUDIO_MPEG,
                CONTENT_DISPOSITION -> s"""attachment; filename="$name"""")
            }
          }).getOrElse(BadRequest)
        }).recoverAll(_ => NotFound) // track ID not found
      })
    })
  }

  def receiveUpload = ServerAction(server => {
    val requestID = server.request
    val transfers = server.socket.fileTransfers
    val parser = transfers parser requestID
    parser.fold[EssentialAction](Action(NotFound))(parser => {
      val maxSize = transfers.maxUploadSize
      log info s"Streaming at most $maxSize for request $requestID"
      val composedParser = parse.maxLength(maxSize.toBytes, parser)
      Action(composedParser)(httpRequest => {
        transfers remove requestID
        httpRequest.body match {
          case Left(tooMuch) =>
            log error s"Entity of ${tooMuch.length} bytes exceeds the max size of ${maxSize.toBytes} bytes for request $requestID"
            EntityTooLarge
          case Right(data) =>
            val fileCount = data.files.size
            log info s"Streamed $fileCount file(s) for request $requestID"
            Ok
        }
      })
    })
  })

  def testUpload: EssentialAction = Logged {
    import com.mle.storage.StorageInt
    val consumer = Iteratee.fold[Array[Byte], Long](0L)((acc, bytes) => acc + bytes.length)
    val (iteratee, enumerator) = Concurrent.joined[Array[Byte]]
    val f = enumerator.run(consumer).map(bytes => log info s"Consumed $bytes bytes")
    val max = 100.megs
    log info s"Using ${max.toBytes.toInt} maxlength"
    val parser = StreamParsers.multiPartBodyParser(iteratee, max)
    val composedParser = parse.maxLength(max.toBytes, parser)
    Action(composedParser)(httpRequest => {
      httpRequest.body match {
        case Left(maxExceeded) =>
          log info s"Exceeded"
          BadRequest("too much")
        case Right(data) =>
          val files = data.files
          files.foreach(file => {
            log info s"Byte streaming complete"
          })
          Ok
      }
    })
  }

  def decode(id: String) = URLDecoder.decode(id, "UTF-8")

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
      uuid <- JsonFutureSocket.tryParseUUID(requestID) //if fileStreams.exists(uuid)
      server <- Servers.clients.find(_.fileTransfers.exists(uuid))
    } yield Server(uuid, server)
  }

  case class Server(request: UUID, socket: PimpSocket)

  def fut[T](body: => T) = Future successful body

  val invalidCredentials = new NoSuchElementException("Invalid credentials")
}
