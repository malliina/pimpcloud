package com.mle.pimpcloud

import com.mle.musicpimp.cloud.PimpSocket
import com.mle.pimpcloud.ws.PhoneClient
import play.api.libs.json.JsValue

/**
 * @author mle
 */
trait MessageMediator {
  def routePhoneMessage(msg: JsValue, client: PhoneClient)

  def routeServerMessage(msg: JsValue, client: PimpSocket)
}

trait MultiSockets {
  def servers: Seq[Int]
  def phones: Seq[Int]

  def onPhoneMessage: Unit
  def onServerMessage: Unit

}