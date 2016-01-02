package com.malliina.musicpimp.messaging

import com.malliina.push.adm.ADMClient
import com.malliina.push.apns.APNSClient
import com.malliina.push.gcm.GCMClient
import com.malliina.push.mpns.MPNSClient
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
  * @author mle
  */
class Pusher(apnsCredentials: APNSCredentials, gcmApiKey: String, admCredentials: ADMCredentials) {
  def this(conf: PushConf) = this(conf.apns, conf.gcmApiKey, conf.adm)

  val apnsHandler = new APNSHandler(new APNSClient(
    apnsCredentials.keyStore,
    apnsCredentials.keyStorePass,
    isSandbox = apnsCredentials.isSandbox))
  val gcmHandler = new GCMHandler(new GCMClient(gcmApiKey))
  val admHandler = new ADMHandler(new ADMClient(
    admCredentials.clientId,
    admCredentials.clientSecret))
  val mpnsHandler = new MPNSHandler(new MPNSClient)

  def push(pushTask: PushTask): Future[PushResult] = {
    val apnsFuture = apnsHandler.push(pushTask.apns)
    val gcmFuture = gcmHandler.push(pushTask.gcm)
    val admFuture = admHandler.push(pushTask.adm)
    val mpnsFuture = mpnsHandler.push(pushTask.mpns)
    for {
      apns <- apnsFuture
      gcm <- gcmFuture
      adm <- admFuture
      mpns <- mpnsFuture
    } yield PushResult(apns, gcm, adm, mpns)
  }
}

object Pusher {
  def fromConf: Pusher = new Pusher(PushConfReader.load)
}
