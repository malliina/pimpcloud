package com.malliina.musicpimp.messaging

import java.security.KeyStore

/**
  * @author mle
  */
case class APNSCredentials(keyStore: KeyStore, keyStorePass: String, isSandbox: Boolean)
