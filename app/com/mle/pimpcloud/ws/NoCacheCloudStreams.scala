package com.mle.pimpcloud.ws

import java.util.UUID

import com.mle.play.streams.StreamParsers
import com.mle.storage.StorageSize
import play.api.http.LazyHttpErrorHandler
import play.api.http.Status._
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee._
import play.api.libs.json.JsValue
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{Result, RequestHeader, BodyParser, MultipartFormData}
import play.core.parsers.Multipart
import play.core.parsers.Multipart._

import scala.concurrent.Future

/**
 * @author Michael
 */
class NoCacheCloudStreams(id: String, channel: Channel[JsValue]) extends CloudStreams[Array[Byte]](id, channel) {
  override def parser(uuid: UUID): Option[BodyParser[MultipartFormData[Unit]]] = {
    get(uuid).map(iteratee => multiPartBodyParser(iteratee, maxUploadSize))
  }

  def multiPartBodyParser[T](iteratee: Iteratee[Array[Byte], T], maxLength: StorageSize): BodyParser[MultipartFormData[T]] =
    multipartFormDataFixed(byteArrayPartHandler(iteratee), maxLength)

  def multipartFormDataFixed[A](filePartHandler: Multipart.PartHandler[FilePart[A]], maxLength: StorageSize): BodyParser[MultipartFormData[A]] = {
    val maxLengthLong = maxLength.toBytes
    BodyParser("multipartFormData") { request =>
      import play.api.libs.iteratee.Execution.Implicits.trampoline

      val parser = Traversable.takeUpTo[Array[Byte]](maxLengthLong).transform(
        Multipart.multipartParser(maxLengthLong.toInt, filePartHandler)(request)
      ).flatMap {
        case d @ Left(r) =>
          log info "Done, got Left..."
          Iteratee.eofOrElse(r)(d)
        case d =>
          log info "Done, check for EOF..."
          checkForEof(request)(d)
      }

      parser.map {
        case Left(tooLarge) =>
          log.error(s"Too large ${tooLarge.header.status}")
          Left(tooLarge)
        case Right(Left(badResult)) =>
          log.error(s"Bad result ${badResult.header.status}")
          Left(badResult)
        case Right(Right(body)) =>
          log.info("Right")
          Right(body)
      }
    }
  }

  def byteArrayPartHandler[T](in: Iteratee[Array[Byte], T]): PartHandler[FilePart[T]] = {
    Multipart.handleFilePart {
      case Multipart.FileInfo(partName, fileName, contentType) =>
        in
    }
  }
  /**
   * Copied from play.api.mvc.ContentTypes.scala for now
   */
  private def checkForEof[A](request: RequestHeader): A => Iteratee[Array[Byte], Either[Result, A]] = { eofValue: A =>
    import play.api.libs.iteratee.Execution.Implicits.trampoline
    def cont: Iteratee[Array[Byte], Either[Result, A]] = Cont {
      case in @ Input.El(e) =>
        val badResult: Future[Result] = createBadResult("Request Entity Too Large", REQUEST_ENTITY_TOO_LARGE)(request)
        log.error("Expected EOF, but no EOF.")
        Iteratee.flatten(badResult.map(r => Done(Left(r), in)))
      case in @ Input.EOF =>
        Done(Right(eofValue), in)
      case Input.Empty =>
        cont
    }
    cont
  }

  /**
   * Copied from play.api.mvc.ContentTypes.scala for now
   */
  private def createBadResult(msg: String, statusCode: Int = BAD_REQUEST): RequestHeader => Future[Result] = { request =>
    LazyHttpErrorHandler.onClientError(request, statusCode, msg)
  }
}
