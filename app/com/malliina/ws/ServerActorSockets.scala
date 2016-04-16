package com.malliina.ws

import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.pimpcloud.WithStorage
import com.malliina.pimpcloud.actors.{ActorStorage, ServersActor}
import play.api.libs.json.JsValue

abstract class ServerActorSockets(val storage: ActorStorage[ServersActor, JsValue, PimpServerSocket])
  extends JsonWebSockets2[PimpServerSocket] with WithStorage[JsValue, PimpServerSocket]
