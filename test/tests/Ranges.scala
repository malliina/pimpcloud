package tests

import com.malliina.play.ContentRange
import com.malliina.storage.StorageInt
import org.scalatest.FunSuite

/**
 * @author Michael
 */
class Ranges extends FunSuite {
  test("ContentRange.all") {
    val size = 5.megs
    val asBytes = 5.megs.toBytes
    val all = ContentRange.all(5.megs)
    assert(all.endExclusive - all.start === asBytes)
  }
}
