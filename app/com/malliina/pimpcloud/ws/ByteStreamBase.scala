package com.malliina.pimpcloud.ws

import com.malliina.play.ContentRange
import play.api.libs.iteratee.Enumerator
import play.api.mvc.{Result, Results}

/**
 * @author mle
 */
trait ByteStreamBase extends StreamBase[Array[Byte]] {
  protected def resultify(enumerator: Enumerator[Array[Byte]], range: ContentRange): Result = {
    val status = if (range.isAll) Results.Ok else Results.PartialContent
    status feed enumerator
  }
}
