package com.malliina.pimpcloud.ws

import java.util.UUID

import akka.stream.Materializer
import akka.stream.scaladsl.SourceQueue
import akka.util.ByteString
import com.malliina.play.models.Username
import com.malliina.play.streams.StreamParsers
import play.api.libs.json.JsValue
import play.api.mvc.{BodyParser, MultipartFormData}

class NoCacheByteStreams(id: Username,
                         channel: SourceQueue[JsValue],
                         val mat: Materializer,
                         val onUpdate: () => Unit)
  extends CloudStreams[ByteString](id, channel, mat)
    with ByteStreamBase {

  override def parser(uuid: UUID): Option[BodyParser[MultipartFormData[_]]] = {
    get(uuid) map { info =>
      StreamParsers.multiPartByteStreaming(bytes => info.channel.offer(Option(bytes)).map(_ => ()), maxUploadSize)(mat)
    }
  }
}
