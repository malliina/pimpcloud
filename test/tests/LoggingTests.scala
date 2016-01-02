package tests

import com.malliina.logbackrx.LogbackUtils
import org.scalatest.FunSuite

/**
 * @author Michael
 */
class LoggingTests extends FunSuite {
  test("cannot retrieve appender from scalatest") {
    val appender = LogbackUtils.appender("RX")
    assert(appender.isEmpty)
  }
}
