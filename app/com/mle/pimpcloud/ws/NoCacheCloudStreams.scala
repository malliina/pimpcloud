package com.mle.pimpcloud.ws

import java.util.UUID

import com.mle.play.streams.StreamParsers
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.JsValue
import play.api.mvc.{BodyParser, MultipartFormData}

/**
 * @author Michael
 */
class NoCacheCloudStreams(id: String, channel: Channel[JsValue]) extends CloudStreams[Array[Byte]](id, channel) {
  override def parser(uuid: UUID): Option[BodyParser[MultipartFormData[Unit]]] = {
    get(uuid).map(iteratee => StreamParsers.multiPartBodyParser(iteratee, maxUploadSize))
  }
}
