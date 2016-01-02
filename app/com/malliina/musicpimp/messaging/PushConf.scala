package com.malliina.musicpimp.messaging

/**
  * @author mle
  */
case class PushConf(apns: APNSCredentials,
                    gcmApiKey: String,
                    adm: ADMCredentials)
