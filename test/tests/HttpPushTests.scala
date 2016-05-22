package tests

import com.malliina.musicpimp.messaging.{APNSRequest, PushConfReader, PushResult, PushTask}
import com.malliina.pimpcloud.CloudLoader
import com.malliina.push.apns.{APNSClient, APNSMessage, APNSToken}
import controllers.Push
import org.specs2.mutable.Specification
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, WithApplicationLoader}

import scala.concurrent.Await

class WithApp extends WithApplicationLoader(new CloudLoader)

class HttpPushTests extends Specification {
//  val tokenString = "193942675140b3d429311de140bd08ff423712ec9c3ea365b12e61b84609afa9"
  val tokenString = "81bae54a590a3ae871408bd565d7e441aa952744770783209b2fd54219e3d9fe"
  val testToken = APNSToken.build(tokenString).get
//  val testToken = APNSToken.build("6c9969eee832f6ed2a11d04d6daa404db13cc3d97f7298f0c042616fc2a5cc34").get
//  val testToken = APNSToken.build("9f3c2f830256954ada78bf56894fa7586307f0eedb7763117c84e0c1eee8347a").get
  val testTask = PushTask(
    Option(
      APNSRequest(
        Seq(testToken),
        APNSMessage.badged("this is a test", badge = 4))),
    None,
    None,
    None,
    None
  )

  "App" should {
    "respond to health check" in new WithApp {
      val response = route(app, FakeRequest(GET, "/health")).get
      status(response) mustEqual 200
    }

    "push a notification" in new WithApp {
      val body = Json.obj(Push.Cmd -> Push.PushValue, Push.Body -> testTask)
      val response = route(app, FakeRequest(POST, "/push").withJsonBody(body)).get
      val result = (contentAsJson(response) \ Push.ResultKey).as[PushResult]
      result.apns.size mustEqual 2
    }

//    "get inactive devices" in {
//      val conf = PushConfReader.load.apns
//      val client = new APNSClient(conf.keyStore, conf.keyStorePass)
//      import concurrent.duration.DurationInt
//      val inactives = Await.result(client.inactiveDevices, 10.seconds)
//      println(inactives)
//      1 mustEqual 1
//    }
  }
}
