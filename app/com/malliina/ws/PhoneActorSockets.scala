package com.malliina.ws

import com.malliina.pimpcloud.WithStorage
import com.malliina.pimpcloud.actors.{ActorStorage, PhonesActor}
import com.malliina.pimpcloud.ws.PhoneClient
import play.api.libs.json.JsValue

abstract class PhoneActorSockets(val storage: ActorStorage[PhonesActor, JsValue, PhoneClient])
  extends JsonWebSockets2[PhoneClient] with WithStorage[JsValue, PhoneClient]
