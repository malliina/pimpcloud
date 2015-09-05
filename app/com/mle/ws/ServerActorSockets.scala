package com.mle.ws

import com.mle.musicpimp.cloud.PimpSocket
import com.mle.pimpcloud.WithStorage
import com.mle.pimpcloud.actors.{ActorStorage, ServersActor}
import play.api.libs.json.JsValue

/**
 * @author mle
 */
abstract class ServerActorSockets(val storage: ActorStorage[ServersActor, JsValue, PimpSocket])
  extends JsonWebSockets2[PimpSocket] with WithStorage[JsValue, PimpSocket]

