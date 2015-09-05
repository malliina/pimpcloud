package tests

import org.scalatest.FunSuite

/**
 * @author mle
 */
class InitTests extends FunSuite{
  test("can do something") {
    def boom(msg: String): Unit = ???
    val a = new A {
      override def send(msg: String): Unit = {
        boom(msg)
      }
    }
    val b = new B {
      override def send(msg: String): Unit = {
        v = msg
        super.send(msg)
      }
    }
  }
}

class A extends DoIt

class B extends DoIt

trait DoIt {
  var v = "init"
  def send(msg: String) = ()
}