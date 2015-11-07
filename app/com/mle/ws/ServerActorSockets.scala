package com.mle.ws

import com.mle.musicpimp.cloud.PimpServerSocket
import com.mle.pimpcloud.WithStorage
import com.mle.pimpcloud.actors.{ActorStorage, ServersActor}
import play.api.libs.json.JsValue

/**
 * @author mle
 */
abstract class ServerActorSockets(val storage: ActorStorage[ServersActor, JsValue, PimpServerSocket])
  extends JsonWebSockets2[PimpServerSocket] with WithStorage[JsValue, PimpServerSocket]
