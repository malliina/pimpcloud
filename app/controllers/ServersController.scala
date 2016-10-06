package controllers

import com.malliina.pimpcloud.auth.CloudAuthentication
import controllers.ServersController.log
import play.api.Logger
import play.api.mvc.{Action, Controller, EssentialAction}

class ServersController(cloudAuth: CloudAuthentication, auth: CloudAuth) extends Controller {

  def receiveUpload = serverAction { server =>
    val requestID = server.request
    log info s"Processing $requestID..."
    val transfers = server.socket.fileTransfers
    val parser = transfers parser requestID
    parser.fold[EssentialAction](Action(NotFound)) { parser =>
      val maxSize = transfers.maxUploadSize
      log info s"Streaming at most $maxSize for request $requestID"
      val composedParser = parse.maxLength(maxSize.toBytes, parser)(auth.mat)
      Action(composedParser) { httpRequest =>
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
      }
    }
  }

  def serverAction(f: Server => EssentialAction): EssentialAction =
    auth.loggedSecureActionAsync(cloudAuth.authServer)(f)
}

object ServersController {
  private val log = Logger(getClass)
}
