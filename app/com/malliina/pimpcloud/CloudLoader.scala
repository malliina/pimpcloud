package com.malliina.pimpcloud

import com.malliina.musicpimp.messaging.Pusher
import com.malliina.play.controllers.AccountForms
import controllers._
import play.api.ApplicationLoader.Context
import play.api.mvc.EssentialFilter
import play.api.{ApplicationLoader, BuiltInComponentsFromContext, LoggerConfigurator}
import play.filters.gzip.GzipFilter
import router.Routes

class CloudLoader extends ApplicationLoader {
  def load(context: Context) = {
    LoggerConfigurator(context.environment.classLoader)
      .foreach(_.configure(context.environment))
    new CloudComponents(context).application
  }
}

class CloudComponents(context: Context) extends BuiltInComponentsFromContext(context) {
  override lazy val httpFilters: Seq[EssentialFilter] = Seq(new GzipFilter())

  // Components
  lazy val auth = new CloudAuth(materializer)
  lazy val pusher = Pusher.fromConf
  val forms = new AccountForms
  // Controllers
  lazy val push = new Push(pusher)
  lazy val (s, ps) = JoinedSockets.joined(materializer)
  lazy val p = new Phones(s, ps, auth)
  lazy val sc = new ServersController(s, auth)
  lazy val aa = new AdminAuth(materializer)
  lazy val l = new Logs(aa)
  lazy val w = new Web(s, auth, forms)
  lazy val us = new UsageStreaming(s, p, ps, sc, aa)
  lazy val as = new Assets(httpErrorHandler)
  lazy val router = new Routes(httpErrorHandler, p, w, push, ps, sc, s, as, us, l, aa)
}
