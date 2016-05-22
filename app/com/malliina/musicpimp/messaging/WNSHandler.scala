package com.malliina.musicpimp.messaging

import com.malliina.push.wns.{WNSClient, WNSResponse}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

class WNSHandler(client: WNSClient) extends PushRequestHandler[WNSRequest, WNSResult] {
  override def push(request: WNSRequest): Future[Seq[WNSResult]] =
    request.message
      .map(message => client.pushAll(request.tokens, message))
      .getOrElse(Future.successful(Nil))
      .map(rs => rs.map(toResult))

  def toResult(response: WNSResponse): WNSResult =
    WNSResult(response.reason, response.description, response.statusCode, response.isSuccess)
}
