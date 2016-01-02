package com.malliina.musicpimp.messaging

import com.malliina.push.adm.ADMClient
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
  * @author mle
  */
class ADMHandler(client: ADMClient) extends PushRequestHandler[ADMRequest, BasicResult] {
  override def push(request: ADMRequest): Future[Seq[BasicResult]] =
    client.pushAll(request.tokens, request.message)
      .map(rs => rs.map(BasicResult.fromResponse))
}
