package com.malliina.pimpcloud.ws

import java.util.UUID

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.play.streams.StreamParsers
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.JsValue
import play.api.mvc.{BodyParser, MultipartFormData}

/**
 * @author Michael
 */
class NoCacheCloudStreams(id: String, channel: Channel[JsValue], val onUpdate: () => Unit)
  extends CloudStreams[Array[Byte]](id, channel) with ByteStreamBase {

  override def parser(uuid: UUID): Option[BodyParser[MultipartFormData[_]]] = {
    get(uuid).map(info => StreamParsers.multiPartChannelStreaming(info.channel, maxUploadSize))
  }
}
