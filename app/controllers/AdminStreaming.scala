package controllers

import com.malliina.maps.{ItemMap, StmItemMap}
import com.malliina.play.controllers.Streaming
import com.malliina.play.http.AuthedRequest
import com.malliina.play.models.Username
import com.malliina.play.ws.JsonSocketClient
import play.api.mvc.{EssentialAction, RequestHeader}
import play.twirl.api.Html
import rx.lang.scala.Subscription

import scala.concurrent.Future

abstract class AdminStreaming(admin: AdminAuth)
  extends Streaming(admin.mat) {

  override val subscriptions: ItemMap[JsonSocketClient[Username], Subscription] =
    StmItemMap.empty[JsonSocketClient[Username], Subscription]

  override def authenticateAsync(req: RequestHeader): Future[AuthedRequest] =
    getOrFail(admin.authenticate(req))

  def navigate(page: => Html): EssentialAction =
    navigate(_ => page)

  def navigate(f: RequestHeader => Html): EssentialAction =
    admin.navigate(f)

  private def getOrFail[T](f: Future[Option[T]]): Future[T] =
    f.flatMap(_.map(Future.successful).getOrElse(Future.failed(new NoSuchElementException)))
}
