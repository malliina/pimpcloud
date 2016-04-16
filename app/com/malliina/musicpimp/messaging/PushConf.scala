package com.malliina.musicpimp.messaging

case class PushConf(apns: APNSCredentials,
                    gcmApiKey: String,
                    adm: ADMCredentials)
