package com.malliina.pimpcloud.ws

import java.util.UUID

import akka.stream.Materializer
import akka.stream.scaladsl.SourceQueue
import akka.util.ByteString
import com.malliina.concurrent.FutureOps
import com.malliina.pimpcloud.ws.NoCacheByteStreams.log
import com.malliina.play.models.Username
import com.malliina.play.streams.StreamParsers
import play.api.Logger
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
      StreamParsers.multiPartByteStreaming(bytes => info.channel
        .offer(Option(bytes))
        .map(_ => ())
        .recoverAll(onOfferError(uuid, _)), maxUploadSize)(mat)
    }
  }

  def onOfferError(uuid: UUID, t: Throwable) = {
    log.error(s"Offer failed for request $uuid", t)
    remove(uuid)
  }
}

object NoCacheByteStreams {
  private val log = Logger(getClass)
}
