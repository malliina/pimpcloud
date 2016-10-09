package tests

import java.nio.file.Paths
import java.util.UUID

import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.ByteString
import com.malliina.http.TrustAllMultipartRequest
import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.pimpcloud.CloudComponents
import com.malliina.pimpcloud.auth.FakeAuth
import com.malliina.pimpcloud.models.{CloudID, TrackID}
import com.malliina.play.models.Username
import com.malliina.play.{ContentRange, Streaming}
import com.malliina.storage.StorageInt
import com.malliina.util.Util
import controllers.Uploads
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.JsValue
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{Controller, MultipartFormData, Request}
import play.api.test.{FakeHeaders, FakeRequest}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class StreamingTests extends BaseSuite with OneAppPerSuite2[CloudComponents] with WithAppComponents {
  implicit val mat = app.materializer
  implicit val ec = mat.executionContext

  val fileName = "10 Right Face - Morning Dew.mp3"
  val file = s"E:\\musik\\Elektroniskt\\A State Of Trance 600 Expedition\\CD 2 - ATB\\$fileName"
  val filePath = Paths get file

  test("upload file") {
    val ctrl = new Uploads

    val fileName = "10 Right Face - Morning Dew.mp3"
    val filePath = s"E:\\musik\\Elektroniskt\\A State Of Trance 600 Expedition\\CD 2 - ATB\\$fileName"
    val path = Paths get filePath
    val tempFile = TemporaryFile(path.toFile)
    val part = FilePart("file", fileName, None, tempFile)
    val files = Seq[FilePart[TemporaryFile]](part)
//    val r: Request[MultipartFormData[TemporaryFile]]
    val multiData = MultipartFormData(Map.empty, files, Nil)
    val req = FakeRequest[MultipartFormData[TemporaryFile]]("POST", "/test", FakeHeaders(Seq("boundary" -> "123456789")), multiData)
//    val req = FakeRequest(method = "POST", path = "/unused").withMultipartFormDataBody(MultipartFormData(Map.empty, files, Nil))
    val r = await(ctrl.up.apply(req))
    val resultAsString = await(r.body.consumeData.map(_.utf8String))
    println(resultAsString)
    //    assert(r.body.contentLength contains 0L)
    assert(r.header.status === 400)
  }

  test("file to source") {
    val (queue, source) = Streaming.sourceQueue[ByteString](app.materializer)
    val byteCalculator: Sink[ByteString, Future[Long]] = Sink.fold[Long, ByteString](0)((acc, bytes) => acc + bytes.length)
    val asyncSink = Flow[ByteString].mapAsync(1)(bytes => queue.offer(Option(bytes)).map(_ => bytes)).toMat(byteCalculator)(Keep.right)
    //    val bytesFuture = source.to(asyncSink).run()
    val bytes = FileIO.fromPath(filePath).runWith(asyncSink)
    Await.result(bytes, 10.seconds)
    println(bytes)
  }

  test("upload") {
    val uri = "http://localhost:9000/track"
    val response = Util.using(new TrustAllMultipartRequest(uri)) { req =>
      req.request.addHeader("request", FakeAuth.FakeUuid.toString)
      req.addFile(filePath)
      req.execute()
    }
    assert(response.getStatusLine.getStatusCode === 404)
  }

  def testServer() = {
    val source = Source.queue[JsValue](100, OverflowStrategy.backpressure)
    val (queue, _) = source.toMat(Sink.asPublisher(fanout = true))(Keep.both).run()(mat)
    val socket = new PimpServerSocket(queue, CloudID("test"), FakeRequest(), mat, () => ())
    socket.streamRange(Track(TrackID(""), "", "", "", 1.second, 1.megs), ContentRange.all(1.megs))
    socket.fileTransfers.parser(UUID.fromString("123"))
  }
}

class TestCtrl(mat: Materializer) extends Controller {
  implicit val ec = mat.executionContext


  //  def hm() = EssentialAction {
  //
  //    val parser = StreamParsers.multiPartByteStreaming(bytes => queue.offer(Option(bytes)).map(_ => ()), 1024.megs)(mat)
  //    Action(parser) { req =>
  //      queue.offer(None)
  //      Ok
  //    }
  //    //    Ok.sendEntity(HttpEntity.Streamed(source, None, None))
  //  }
}
