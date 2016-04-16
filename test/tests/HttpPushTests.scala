package tests

import com.malliina.musicpimp.messaging.{APNSRequest, PushResult, PushTask}
import com.malliina.pimpcloud.CloudLoader
import com.malliina.push.apns.{APNSMessage, APNSToken}
import controllers.Push
import org.specs2.mutable.Specification
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, WithApplicationLoader}

class WithApp extends WithApplicationLoader(new CloudLoader)

class HttpPushTests extends Specification {
  val testToken = APNSToken.build("6c9969eee832f6ed2a11d04d6daa404db13cc3d97f7298f0c042616fc2a5cc34").get
//  val testToken = APNSToken.build("9f3c2f830256954ada78bf56894fa7586307f0eedb7763117c84e0c1eee8347a").get
  val testTask = PushTask(
    Option(
      APNSRequest(
        Seq(testToken),
        APNSMessage.badged("this is a test", badge = 4))),
    None,
    None,
    None
  )

  "App" should {
    "respond to health check" in new WithApp {
      val response = route(FakeRequest(GET, "/health")).get
      status(response) mustEqual 200
    }

    "push a notification" in new WithApp {
      val body = Json.obj(Push.Cmd -> Push.PushValue, Push.Body -> testTask)
      val response = route(FakeRequest(POST, "/push").withJsonBody(body)).get
      val result = (contentAsJson(response) \ Push.ResultKey).as[PushResult]
      result.apns.size mustEqual 1
    }
  }
}
