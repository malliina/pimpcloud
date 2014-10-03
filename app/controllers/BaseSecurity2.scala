package controllers

import com.mle.concurrent.FutureImplicits.RichFuture
import com.mle.play.controllers.{AuthRequest, BaseSecurity}
import play.api.libs.iteratee.{Done, Input, Iteratee}
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * @author Michael
 */
trait BaseSecurity2 extends BaseSecurity {
  /**
   *
   * @param authFunction
   * @param authAction
   * @tparam U type of user
   * @return
   */
  def LoggedSecureAction[U](authFunction: RequestHeader => Option[U])(authAction: U => EssentialAction): EssentialAction =
    Security.Authenticated(req => authFunction(req), req => onUnauthorized(req))(user => Logged(authAction(user)))

  def AuthActionAsync(f: AuthRequest[AnyContent] => Future[Result]) =
    AuthenticatedLogged(user => Action.async(req => f(new AuthRequest(user.user, req, user.cookie))))

  def LoggedSecureActionAsync[U](authFunction: RequestHeader => Future[U])(authAction: U => EssentialAction) =
    AuthenticatedAsync(authFunction, req => onUnauthorized(req))(user => Logged(authAction(user)))

  def AuthenticatedAsync[A](authFunction: RequestHeader => Future[A],
                            onUnauthorized: RequestHeader => Result)(action: A => EssentialAction): EssentialAction = {
    val f2: RequestHeader => Future[Option[A]] = req => authFunction(req).map(a => Some(a)).recoverAll(_ => None)
    AuthenticatedAsync2(f2, onUnauthorized)(action)
  }

  def AuthenticatedAsync2[A](authFunction: RequestHeader => Future[Option[A]],
                             onUnauthorized: RequestHeader => Result)(action: A => EssentialAction): EssentialAction = {
    EssentialAction(request => {
      val futureIteratee: Future[Iteratee[Array[Byte], Result]] = authFunction(request)
        .map(userOpt => userOpt.map(user => action(user)(request))
        .getOrElse(Done(onUnauthorized(request), Input.Empty)))
      Iteratee flatten futureIteratee
    })
  }
}
