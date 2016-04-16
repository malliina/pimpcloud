package com.malliina.musicpimp.messaging

import com.malliina.push.apns.APNSClient
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

class APNSHandler(client: APNSClient) extends PushRequestHandler[APNSRequest, APNSResult] {
  override def push(request: APNSRequest): Future[Seq[APNSResult]] =
    client.pushAll(request.tokens, request.message)
      .map(_.map(an => APNSResult.fromAPNS(an)))
}
