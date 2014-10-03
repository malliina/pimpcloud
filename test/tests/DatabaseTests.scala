package tests

import com.mle.pimpcloud.db.CloudDatabase
import org.scalatest.{BeforeAndAfterEach, FunSuite}

/**
 * @author Michael
 */
class DatabaseTests extends FunSuite with BeforeAndAfterEach {
  val db = new CloudDatabase("testdb")
  val testID = "A"

  test("cloud database") {
    db tryAdd testID
    assert(db exists testID)
  }
  test("cloud identity store") {
    val id = db.generateAndSave()
    assert(id.right.exists(_.length == 5))
  }

  override protected def beforeEach(): Unit = {
    db.init()
  }

  override protected def afterEach(): Unit = {
    db.dropAll()
  }
}
