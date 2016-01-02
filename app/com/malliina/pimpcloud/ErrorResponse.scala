package com.malliina.pimpcloud

import play.api.http.Writeable
import play.api.libs.json.Json

/**
 * @author mle
 */
case class ErrorResponse(errors: Seq[ErrorMessage])

object ErrorResponse {
  implicit val json = Json.format[ErrorResponse]
  implicit val writeable = Writeable.writeableOf_JsValue.map[ErrorResponse](Json.toJson(_))
}
