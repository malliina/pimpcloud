package controllers

import akka.stream.Materializer
import com.malliina.pimpcloud.ws.PhoneSockets
import play.api.libs.json.JsValue
import play.api.mvc.Call
import rx.lang.scala.Observable

class UsageStreaming(servers: Servers,
                     phones: Phones,
                     phoneSockets: PhoneSockets,
                     serversController: ServersController,
                     adminAuth: AdminAuth,
                     val mat: Materializer) extends AdminStreaming(adminAuth) {
  override def jsonEvents: Observable[JsValue] = servers.usersJson merge phoneSockets.usersJson merge servers.uuidsJson

  override def openSocketCall: Call = routes.UsageStreaming.openSocket()

  def index = navigate(implicit req => views.html.admin(wsUrl(req)))
}
