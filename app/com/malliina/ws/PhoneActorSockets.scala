package com.malliina.ws

import com.malliina.pimpcloud.ws.StorageSocket
import com.malliina.play.ws.JsonWebSockets

abstract class PhoneActorSockets
  extends JsonWebSockets
    with StorageSocket
