package tests

import com.mle.logbackrx.LogbackUtils
import com.mle.util.Log
import org.scalatest.FunSuite

/**
 * @author Michael
 */
class LoggingTests extends FunSuite with Log {
  test("cannot retrieve appender from scalatest") {
    log info "tyo"
    val appender = LogbackUtils.appender("RX")
    assert(appender.isEmpty)
  }
}
