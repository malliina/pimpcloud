package controllers

import com.mle.musicpimp.json.JsonStrings
import com.mle.musicpimp.json.JsonStrings._
import com.mle.pimpcloud.ws.StreamData
import com.mle.ws.JsonFutureSocket
import play.api.libs.json.{Json, JsValue}
import play.api.mvc.{Action, EssentialAction, RequestHeader}
import rx.lang.scala.Observable
import rx.lang.scala.subjects.BehaviorSubject

/**
 * @author mle
 */
class ServersController(servers: Servers) extends Secured {

  def receiveUpload = serverAction(server => {
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

  def serverAction(f: Server => EssentialAction) = LoggedSecureAction(authServer)(f)

  def authServer(req: RequestHeader): Option[Server] = {
    for {
      requestID <- req.headers get JsonStrings.REQUEST_ID
      uuid <- JsonFutureSocket.tryParseUUID(requestID)
      server <- servers.clients.find(_.fileTransfers.exists(uuid))
    } yield Server(uuid, server)
  }
}
