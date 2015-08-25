package tests

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Play

/**
 * @author mle
 */
class AppTests extends PlaySpec with OneAppPerSuite {

  "app" must {
    "start the FakeApplication" in {
      Play.maybeApplication mustBe Some(app)
    }
  }
}
