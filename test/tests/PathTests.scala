package tests

import java.nio.file.Paths

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

  test("can do apostrophes") {
    val p = "Dire+Straits%5C%281979%29+Communiqu%C3%A9%5C03+-+Where+Do+You+Think+You%27re+Going..mp3"
    val str = Phones.decode(p)
    Paths.get(str)
    val original = "é"
    Paths.get(original)
    assert(1 === 1)
  }
}
