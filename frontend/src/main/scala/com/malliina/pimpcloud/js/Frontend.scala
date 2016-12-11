package com.malliina.pimpcloud.js

import java.util.UUID

import org.scalajs.dom
import org.scalajs.dom.CloseEvent
import org.scalajs.dom.raw.{ErrorEvent, Event, MessageEvent}
import org.scalajs.jquery.{JQueryEventObject, jQuery}
import upickle.Invalid

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.{JSApp, JSON}
import scalatags.Text.all._

object Frontend extends JSApp {
  var app: Option[LogsFrontend] = None

  @JSExport
  override def main() = {
    app = Option(new LogsFrontend)
  }
}

case class JVMLogEntry(level: String,
                       message: String,
                       loggerName: String,
                       threadName: String,
                       timeFormatted: String,
                       stackTrace: Option[String] = None)

case class Command(cmd: String)

object Command {
  val Subscribe = apply("subscribe")
}

class LogsFrontend {
  val tableContent = elem("logTableBody")
  val statusElem = elem("status")

  val socket: dom.WebSocket = openSocket()

  def openSocket() = {
    val pathAndQuery = "/admin/ws?f=json"
    val wsUrl = s"$wsBaseUrl$pathAndQuery"
    val socket = new dom.WebSocket(wsUrl)
    socket.onopen = (_: Event) => {
      socket.send(PimpJSON.write(Command.Subscribe))
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
    val event = JSON.parse(msg.data.toString)
    if (event.event.toString == "ping") {
    } else {
      handlePayload(JSON.stringify(event))
    }
  }

  def handlePayload(payload: String) = {
    PimpJSON.validate[Seq[JVMLogEntry]](payload).fold(
      invalid => onInvalidData(invalid),
      entries => entries foreach prepend
    )
  }

  def prepend(entry: JVMLogEntry) = {
    val trc = entry.level match {
      case "ERROR" => "danger"
      case "WARN" => "warning"
      case _ => ""
    }
    val level = entry.level
    val entryId = UUID.randomUUID().toString take 5
    val rowId = s"row-$entryId"
    val linkId = s"link-$entryId"
    val levelCell: Modifier = entry.stackTrace
      .map(_ => a(href := "#", id := linkId)(level))
      .getOrElse(level)

    entry.stackTrace foreach { stackTrace =>
      val errorRow = tr(style := "display: none", id := s"$rowId")(
        td(colspan := "5")(pre(stackTrace))
      )
      tableContent prepend errorRow.toString()
    }
    val row = tr(`class` := trc)(
      td(`class` := "col-md-1")(entry.timeFormatted),
      td(entry.message),
      td(entry.loggerName),
      td(entry.threadName),
      td(levelCell)
    )
    tableContent prepend row.toString()
    elem(linkId).click((_: JQueryEventObject) => toggle(rowId))
  }

  def onInvalidData(invalid: Invalid): PartialFunction[Invalid, Unit] = {
    case Invalid.Data(jsValue, errorMessage) =>
      println(s"JSON failed to parse: '$errorMessage' in value '$jsValue'")
    case Invalid.Json(errorMessage, in) =>
      println(s"Not JSON, '$errorMessage' in value '$in'")
  }

  def toggle(row: String) = {
    global.jQuery(s"#$row").toggle()
    false
  }

  def setFeedback(feedback: String) = statusElem html feedback

  def elem(id: String) = jQuery(s"#$id")

  def global = js.Dynamic.global
}
