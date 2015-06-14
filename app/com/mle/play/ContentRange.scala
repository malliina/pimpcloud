package com.mle.play

import com.mle.json.JsonFormats
import com.mle.storage.StorageSize
import play.api.http.HeaderNames._
import play.api.libs.json.Json
import play.api.mvc.RequestHeader

import scala.util.{Failure, Try}

/**
 * @author mle
 */
case class ContentRange(start: Int, endInclusive: Int, size: StorageSize) {
  def endExclusive = endInclusive + 1

  def contentLength = endExclusive - start

  def contentRange = s"${ContentRange.BYTES} $start-$endInclusive/$size"

  def isAll = start == 0 && endInclusive == size.toBytes.toInt - 1
}

object ContentRange {

  implicit val ssf = JsonFormats.storageSizeFormat
  implicit val json = Json.format[ContentRange]

  val BYTES = "bytes"

  def all(size: StorageSize) = ContentRange(0, size.toBytes.toInt - 1, size)

  def fromHeaderOrAll(request: RequestHeader, size: StorageSize): ContentRange =
    fromHeader(request, size) getOrElse all(size)

  def fromHeader(request: RequestHeader, size: StorageSize): Try[ContentRange] = {
    request.headers.get(RANGE)
      .map(range => fromHeader(range, size))
      .getOrElse(Failure(new IllegalArgumentException(s"Missing $RANGE header.")))
  }

  def fromHeader(headerValue: String, size: StorageSize): Try[ContentRange] = Try {
    val sizeBytes = size.toBytes.toInt
    val prefix = s"$BYTES="
    if (headerValue startsWith prefix) {
      val suffix = headerValue substring prefix.length
      val (start, end) =
        if (suffix startsWith "-") {
          (sizeBytes - suffix.tail.toInt, sizeBytes - 1)
        } else if (suffix endsWith "-") {
          (suffix.init.toInt, sizeBytes - 1)
        } else {
          val Array(start, endInclusive) = suffix split "-"
          (start.toInt, endInclusive.toInt)
        }
      if (end >= start) {
        ContentRange(start, end, size)
      } else {
        throw new IllegalArgumentException(s"End must be greater or equal to start: $headerValue")
      }
    } else {
      throw new IllegalArgumentException(s"Does not start with '$prefix': $headerValue")
    }
  }
}
