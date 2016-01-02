package com.malliina.pimpcloud

import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.pimpcloud.ws.PhoneClient
import play.api.libs.json.JsValue

/**
  * @author mle
  */
trait MessageMediator {
  def routePhoneMessage(msg: JsValue, client: PhoneClient)

  def routeServerMessage(msg: JsValue, client: PimpServerSocket)
}

trait MultiSockets {
  def servers: Seq[Int]

  def phones: Seq[Int]

  def onPhoneMessage: Unit

  def onServerMessage: Unit

}
