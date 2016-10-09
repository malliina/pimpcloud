package tests

import java.nio.file.{Path, Paths}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import controllers.Uploads
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{BodyParser, MultipartFormData}
import play.api.test.{FakeHeaders, FakeRequest}
import play.core.parsers.Multipart

class UploadTests extends BaseSuite {
  val ctrl = new Uploads()

  val as = ActorSystem("test")
  implicit val mat = ActorMaterializer()(as)
  implicit val ec = mat.executionContext

  test("can multipart") {
    val fileName = "10 Right Face - Morning Dew.mp3"
    val filePath = s"E:\\musik\\Elektroniskt\\A State Of Trance 600 Expedition\\CD 2 - ATB\\$fileName"
    val req = multipartRequest(Paths get filePath)
    val r = await(ctrl.up.apply(req))
    val bodyAsString = await(r.body.consumeData.map(_.utf8String))
    assert(r.header.status === 200)
  }

  def multipartRequest(file: Path) = {
    val tempFile = TemporaryFile(file.toFile)
    val part = FilePart("file", file.getFileName.toString, None, tempFile)
    val files = Seq[FilePart[TemporaryFile]](part)
    val multiData = MultipartFormData(Map.empty, files, Nil)
    FakeRequest("POST", "/test", FakeHeaders(Seq("boundary" -> "123456789")), multiData)
  }

  /**
    * Parse the content as multipart/form-data
    */
  def multipartFormData: BodyParser[MultipartFormData[TemporaryFile]] =
  multipartFormData(Multipart.handleFilePartAsTemporaryFile)

  val defaultLengths: Int = 512 * 1024 * 1024

  /**
    * Parse the content as multipart/form-data
    *
    * @param filePartHandler Handles file parts.
    */
  def multipartFormData[A](filePartHandler: Multipart.FilePartHandler[A], maxLength: Long = defaultLengths): BodyParser[MultipartFormData[A]] = {
    BodyParser("multipartFormData") { request =>
      Multipart.multipartParser(defaultLengths, filePartHandler).apply(request)
    }
  }
}
