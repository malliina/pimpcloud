package controllers

import com.mle.play.ws.WebSocketBase
import rx.lang.scala.subjects.BehaviorSubject

/**
 * @author Michael
 */
trait UsersEvents extends WebSocketBase {
  val users = BehaviorSubject[Seq[Client]](Nil)

  override abstract def onConnect(client: Client): Unit = {
    super.onConnect(client)
    users onNext clients
  }

  override abstract def onDisconnect(client: Client): Unit = {
    super.onDisconnect(client)
    users onNext clients
  }
}
