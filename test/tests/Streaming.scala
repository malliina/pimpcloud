package tests

import java.nio.file.{Files, Path, Paths}
import java.util.UUID

import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.ByteString
import com.malliina.http.{MultipartRequest, TrustAllMultipartRequest}
import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.pimpcloud.CloudComponents
import com.malliina.pimpcloud.auth.FakeAuth
import com.malliina.pimpcloud.models.{CloudID, TrackID}
import com.malliina.play.{ContentRange, Streaming}
import com.malliina.storage.StorageInt
import com.malliina.util.Util
import com.malliina.util.Util._
import org.apache.commons.io.FileUtils
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import play.api.libs.json.JsValue
import play.api.mvc.Controller
import play.api.test.FakeRequest

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class StreamingTests extends BaseSuite with OneAppPerSuite2[CloudComponents] with WithAppComponents {
  implicit val mat = app.materializer
  implicit val ec = mat.executionContext

  val fileName = "01 Atb - Ecstacy (Intro Edit).mp3"
  val file = s"E:\\musik\\Elektroniskt\\A State Of Trance 600 Expedition\\CD 2 - ATB\\$fileName"
  val filePath = Paths get file

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
    // Register file listener
    val listenUri = "http://localhost:9000/testfile"
    val client = HttpClientBuilder.create().build()
    val listenRequest = new HttpGet(listenUri)
    val listenResponse = client.execute(listenRequest)
    assert(listenResponse.getStatusLine.getStatusCode === 200)
    Thread.sleep(200)
    // Upload file
    val uploadUri = "http://localhost:9000/testup"
    val response = Util.using(new TrustAllMultipartRequest(uploadUri)) { req =>
      req.request.addHeader("request", FakeAuth.FakeUuid.toString)
      req.addFile(filePath)
      req.execute()
    }
    assert(response.getStatusLine.getStatusCode === 200)
  }


  def multiPartUpload(uri: String, tempFile: Path) {
    val file = ensureTestMp3Exists(tempFile)
    using(new MultipartRequest(uri)) { req =>
      req.setAuth("admin", "test")
      req addFile file
      val response = req.execute()
      val statusCode = response.getStatusLine.getStatusCode
      assert(statusCode === 200)
    }
  }

  def ensureTestMp3Exists(tempFile: Path): Path = {
    if (!Files.exists(tempFile)) {
      val dest = Files.createTempFile(null, null)
      val resourceURL = Util.resourceOpt(fileName)
      val url = resourceURL.getOrElse(throw new Exception(s"Resource not found: " + fileName))
      FileUtils.copyURLToFile(url, dest.toFile)
      if (!Files.exists(dest)) {
        throw new Exception(s"Unable to access $dest")
      }
      dest
    } else {
      tempFile
    }
  }

  def testServer() = {
    val source = Source.queue[JsValue](100, OverflowStrategy.backpressure)
    val (queue, _) = source.toMat(Sink.asPublisher(fanout = true))(Keep.both).run()(mat)
    val socket = new PimpServerSocket(queue, CloudID("test"), FakeRequest(), mat, () => ())
    socket.requestTrack(Track(TrackID(""), "", "", "", 1.second, 1.megs), ContentRange.all(1.megs), FakeRequest())
    socket.fileTransfers.parser(UUID.fromString("123"))
  }
}

class TestCtrl(mat: Materializer) extends Controller {
  implicit val ec = mat.executionContext


  //  def hm() = EssentialAction {
  //
  //    val parser = TestParsers.multiPartByteStreaming(bytes => queue.offer(Option(bytes)).map(_ => ()), 1024.megs)(mat)
  //    Action(parser) { req =>
  //      queue.offer(None)
  //      Ok
  //    }
  //    //    Ok.sendEntity(HttpEntity.Streamed(source, None, None))
  //  }
}
