package controllers

import java.util.UUID

import akka.stream.Materializer
import com.malliina.storage.StorageLong
import com.malliina.ws.Streamer
import controllers.StreamReceiver.log
import play.api.Logger
import play.api.mvc.{Action, BodyParser, Controller, MultipartFormData}

class StreamReceiver(mat: Materializer) extends Controller {

  def receiveStream(parser: BodyParser[MultipartFormData[Long]],
                    transfers: Streamer,
                    requestID: UUID) = {
    val maxSize = transfers.maxUploadSize
    log debug s"Streaming at most $maxSize for request $requestID"
    val composedParser = parse.maxLength(maxSize.toBytes, parser)(mat)
    Action(composedParser) { parsedRequest =>
      transfers.remove(requestID, isCanceled = false)
      parsedRequest.body.fold(
        tooMuch => {
          log error s"Max size of ${tooMuch.length} exceeded for request $requestID"
          EntityTooLarge
        },
        data => {
          val streamedSize = data.files.foldLeft(0L)((acc, part) => acc + part.ref).bytes
          val fileCount = data.files.size
          val fileDesc = if (fileCount > 1) "files" else "file"
          log info s"Streamed $streamedSize in $fileCount $fileDesc for request $requestID"
          Ok
        })
    }
  }
}

object StreamReceiver {
  private val log = Logger(getClass)
}
