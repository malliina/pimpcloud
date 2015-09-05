package com.mle.ws

import com.mle.pimpcloud.WithStorage
import com.mle.pimpcloud.actors.{ActorStorage, PhonesActor}
import com.mle.pimpcloud.ws.PhoneClient
import play.api.libs.json.JsValue

/**
 * @author mle
 */
abstract class PhoneActorSockets(val storage: ActorStorage[PhonesActor, JsValue, PhoneClient])
  extends JsonWebSockets2[PhoneClient] with WithStorage[JsValue, PhoneClient]
