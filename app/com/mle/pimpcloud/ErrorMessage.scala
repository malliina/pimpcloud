package com.mle.pimpcloud

import play.api.libs.json.Json

/**
 * @author mle
 */
case class ErrorMessage(message: String)

object ErrorMessage {
  implicit val json = Json.format[ErrorMessage]
}
