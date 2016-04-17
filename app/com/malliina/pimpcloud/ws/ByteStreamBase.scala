package com.malliina.pimpcloud.ws

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.malliina.play.ContentRange
import play.api.http.HttpEntity
import play.api.mvc.{Result, Results}

trait ByteStreamBase extends StreamBase[ByteString] {
  protected def resultify(source: Source[ByteString, NotUsed], range: ContentRange): Result = {
    val status = if (range.isAll) Results.Ok else Results.PartialContent
    status.sendEntity(HttpEntity.Streamed(source, Option(range.contentLength.toLong), None))
  }
}
