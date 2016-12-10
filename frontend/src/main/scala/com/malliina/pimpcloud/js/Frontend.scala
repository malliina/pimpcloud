package com.malliina.pimpcloud.js

import org.scalajs.dom
import org.scalajs.dom.CloseEvent
import org.scalajs.dom.raw.{ErrorEvent, Event, MessageEvent}
import org.scalajs.jquery.jQuery

import scala.scalajs.js
import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport
import scalatags.Text.all._

object Frontend extends JSApp {
  var app: Option[LogsFrontend] = None

  @JSExport
  override def main() = {
    app = Option(new LogsFrontend)
  }
}

@js.native
trait LogEntry extends js.Object {
  def level: String = js.native

  def message: String = js.native

  def loggerName: String = js.native

  def threadName: String = js.native

  def timeFormatted: String = js.native

  def stackTrace: js.UndefOr[String] = js.native
}

class LogsFrontend {
  // TODO fix this
  var rowCounter = 0

  val tableContent = elem("logTableBody")
  val statusElem = elem("status")

  val socket: dom.WebSocket = openSocket()

  def openSocket() = {
    val pathAndQuery = "/admin/ws?f=json"
    val wsUrl = s"$wsBaseUrl$pathAndQuery"
    val socket = new dom.WebSocket(wsUrl)
    socket.onopen = (event: Event) => {
      socket.send("""{"cmd":"subscribe"}""")
      setFeedback("Connected.")
    }
    socket.onmessage = (event: MessageEvent) => {
      onMessage(event)
    }
    socket.onclose = (_: CloseEvent) => setFeedback("Connection closed.")
    socket.onerror = (_: ErrorEvent) => setFeedback("Connection error.")
    socket
  }

  def wsBaseUrl = {
    val location = dom.window.location
    val wsProto = if (location.protocol == "http:") "ws" else "wss"
    s"$wsProto://${location.host}"
  }

  def onMessage(msg: MessageEvent) = {
    val event = jQuery parseJSON msg.data.toString
    if (event.event.toString == "ping") {
    } else {
      prependAll(event)
    }
  }

  def prependAll(msg: js.Dynamic) = {
    val entries = msg.asInstanceOf[js.Array[LogEntry]]
    entries foreach prepend
  }

  def prepend(entry: LogEntry) = {
    val trc = entry.level match {
      case "ERROR" => "danger"
      case "WARN" => "warning"
      case _ => ""
    }
    rowCounter += 1
    val level = entry.level
    val levelContent: Modifier = entry.stackTrace.toOption
      .map(_ => a(href := "#", onclick := s"return toggle($rowCounter)")(level))
      .getOrElse(level)

    entry.stackTrace.toOption foreach { stackTrace =>
      val errorRow = tr(style := "display: none", id := s"row$rowCounter")(
        td(colspan := "5")(pre(stackTrace))
      )
      tableContent prepend errorRow.toString()
    }
    val row = tr(`class` := trc)(
      td(`class` := "col-md-1")(entry.timeFormatted),
      td(entry.message),
      td(entry.loggerName),
      td(entry.threadName),
      td(levelContent)
    )
    tableContent prepend row.toString()
  }

  def toggle(row: Int) = {
    global.jQuery(s"row$row").toggle()
    false
  }

  def setFeedback(feedback: String) = statusElem html feedback

  def elem(id: String) = jQuery(s"#$id")

  def global = js.Dynamic.global
}
