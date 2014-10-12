package controllers

import com.mle.pimpcloud.ws.PhoneSockets
import play.api.libs.json.JsValue
import play.api.mvc.Call
import rx.lang.scala.Observable

/**
 * @author Michael
 */
object UsageStreaming extends AdminStreaming {
  override def jsonEvents: Observable[JsValue] = Servers.usersJson merge PhoneSockets.usersJson merge Phones.fileUploads.uuidsJson

  override def openSocketCall: Call = routes.UsageStreaming.openSocket()

  def index = navigate(implicit req => views.html.admin())
}
