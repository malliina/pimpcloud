package com.malliina.musicpimp.messaging

import com.malliina.push.mpns.MPNSClient
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

class MPNSHandler(client: MPNSClient) extends PushRequestHandler[MPNSRequest, BasicResult] {
  def push(request: MPNSRequest): Future[Seq[BasicResult]] = {
    request.message
      .map(message => client.pushAll(request.tokens, message))
      .getOrElse(Future.successful(Nil))
      .map(rs => rs.map(BasicResult.fromResponse))
  }
}
