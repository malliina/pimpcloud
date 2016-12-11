package com.malliina.pimpcloud.js

import org.scalajs.dom

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

object Frontend extends JSApp {
  var app: Option[SocketJS] = None

  @JSExport
  override def main() = {
    val path = dom.window.location.pathname
    app = path match {
      case "/admin/logs" =>
        Option(new LogsJS)
      case "/admin" =>
        Option(new AdminJS)
      case _ =>
        None
    }
  }
}
