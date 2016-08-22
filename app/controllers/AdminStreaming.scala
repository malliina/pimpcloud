package controllers

import com.malliina.maps.{ItemMap, StmItemMap}
import com.malliina.play.controllers.Streaming
import com.malliina.play.http.AuthResult
import com.malliina.play.ws.WebSocketClient
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Controller, EssentialAction, RequestHeader}
import play.twirl.api.Html
import rx.lang.scala.Subscription

import scala.concurrent.Future

abstract class AdminStreaming(adminAuth: AdminAuth) extends Controller with Streaming {
  override val subscriptions: ItemMap[WebSocketClient, Subscription] = StmItemMap.empty[WebSocketClient, Subscription]

  override def authenticateAsync(req: RequestHeader): Future[AuthResult] =
    getOrFail(adminAuth.authenticate(req))

  def navigate(page: => Html): EssentialAction =
    navigate(_ => page)

  def navigate(f: RequestHeader => Html): EssentialAction =
    adminAuth.AuthAction(req => Ok(f(req)))

  private def getOrFail[T](f: Future[Option[T]]): Future[T] =
    f.flatMap(_.map(Future.successful).getOrElse(Future.failed(new NoSuchElementException)))
}
