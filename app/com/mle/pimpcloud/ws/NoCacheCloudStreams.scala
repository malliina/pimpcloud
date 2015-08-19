package com.mle.pimpcloud.ws

import java.util.UUID

import com.mle.play.concurrent.ExecutionContexts.synchronousIO
import com.mle.play.streams.StreamParsers
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.JsValue
import play.api.mvc.{BodyParser, MultipartFormData}

/**
 * @author Michael
 */
class NoCacheCloudStreams(id: String, channel: Channel[JsValue])
  extends CloudStreams[Array[Byte]](id, channel) with ByteStreamBase {

  override def parser(uuid: UUID): Option[BodyParser[MultipartFormData[_]]] = {
    get(uuid).map(info => StreamParsers.multiPartChannelStreaming(info.channel, maxUploadSize))
  }
}
