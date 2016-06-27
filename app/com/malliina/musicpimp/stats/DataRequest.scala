package com.malliina.musicpimp.stats

import com.malliina.musicpimp.models.User
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc.{QueryStringBindable, RequestHeader}

/**
  *
  * @param username relevant user
  * @param from     start index of results, inclusive
  * @param until    end index, exclusive
  */
case class DataRequest(username: User, from: Int, until: Int) {
  val maxItems = math.max(0, until - from)
}

object DataRequest {
  implicit val json = Json.format[DataRequest]

  val DefaultItemCount = 100
  val From = "from"
  val Until = "until"
  val Page = "page"
  val PageSize = "pageSize"

  val binder = QueryStringBindable.bindableInt

  def default(user: User): DataRequest =
    apply(user, DefaultItemCount)

  def apply(user: User, items: Int): DataRequest =
    apply(user, 0, items)

  def fromRequest(request: AuthenticatedRequest[_, User]): Either[String, DataRequest] =
    fromRequest(request.user, request)

  def fromRequest(user: User, request: RequestHeader): Either[String, DataRequest] =
    ItemLimits.fromRequest(request).right map { limits =>
      DataRequest(user, limits.from, limits.until)
    }

  def fromJson(user: User, body: JsValue) =
    ItemLimits.fromJson(body) map { limits =>
      DataRequest(user, limits.from, limits.until)
    }
}
