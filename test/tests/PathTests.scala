package tests

import controllers.Phones
import org.scalatest.FunSuite

/**
 * @author mle
 */
class PathTests extends FunSuite {
  test("Paths.get fails for invalid paths") {
    val str = "?"
    assert(Phones.path(str).isFailure)
  }

  test("can encode") {
    val original = "?"
    val encoded = Phones.encode(original)
    val decoded = Phones.decode(encoded)
    assert(decoded === original)
  }
}
