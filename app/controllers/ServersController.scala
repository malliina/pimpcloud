package controllers

import java.util.UUID

import akka.stream.Materializer
import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.musicpimp.json.JsonStrings
import com.malliina.ws.JsonFutureSocket
import controllers.ServersController.log
import play.api.Logger
import play.api.mvc.{Action, EssentialAction, RequestHeader}

import scala.concurrent.Future

class ServersController(servers: Servers, val mat: Materializer) extends Secured {

  def receiveUpload = serverAction(server => {
    val requestID = server.request
    val transfers = server.socket.fileTransfers
    val parser = transfers parser requestID
    parser.fold[EssentialAction](Action(NotFound))(parser => {
      val maxSize = transfers.maxUploadSize
      log info s"Streaming at most $maxSize for request $requestID"
      val composedParser = parse.maxLength(maxSize.toBytes, parser)(mat)
      Action(composedParser)(httpRequest => {
        transfers remove requestID
        httpRequest.body match {
          case Left(tooMuch) =>
            log error s"Max size of ${tooMuch.length} exceeded for request $requestID"
            EntityTooLarge
          case Right(data) =>
            val fileCount = data.files.size
            log info s"Streamed $fileCount file(s) for request $requestID"
            Ok
        }
      })
    })
  })

  def serverAction(f: Server => EssentialAction) = LoggedSecureActionAsync(authServer)(f)

  def authServer(req: RequestHeader): Future[Server] = {
    val uuidOpt = for {
      requestID <- req.headers get JsonStrings.REQUEST_ID
      uuid <- JsonFutureSocket.tryParseUUID(requestID)
    } yield uuid
    implicit val ec = mat.executionContext
    for {
      uuid <- toFuture(uuidOpt)
      ss <- servers.connectedServers
      server <- toFuture(findServer(ss, uuid))
    } yield server
  }

  def findServer(ss: Set[PimpServerSocket], uuid: UUID): Option[Server] =
    ss.find(_.fileTransfers.exists(uuid)).map(s => Server(uuid, s))

  def toFuture[T](opt: Option[T]): Future[T] = {
    opt.map(Future.successful).getOrElse(Future.failed(new NoSuchElementException))
  }
}

object ServersController {
  private val log = Logger(getClass)
}
