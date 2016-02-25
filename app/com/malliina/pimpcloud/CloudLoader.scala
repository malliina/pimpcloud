package com.malliina.pimpcloud

import com.malliina.musicpimp.messaging.Pusher
import controllers._
import play.api.ApplicationLoader.Context
import play.api.mvc.EssentialFilter
import play.api.{ApplicationLoader, BuiltInComponentsFromContext, Logger}
import play.filters.gzip.GzipFilter
import router.Routes

class CloudLoader extends ApplicationLoader {
  def load(context: Context) = {
    Logger.configure(context.environment)
    new CloudComponents(context).application
  }
}

class CloudComponents(context: Context) extends BuiltInComponentsFromContext(context) {
  override lazy val httpFilters: Seq[EssentialFilter] = Seq(new GzipFilter())

  lazy val pusher = Pusher.fromConf
  // Controllers
  lazy val push = new Push(pusher)
  lazy val (s, ps) = JoinedSockets.joined(actorSystem)
  lazy val p = new Phones(s, ps)
  lazy val sc = new ServersController(s)
  lazy val aa = new AdminAuth
  lazy val l = new Logs(aa)
  lazy val w = new Web(s)
  lazy val us = new UsageStreaming(s, p, ps, sc, aa)
  lazy val as = new Assets(httpErrorHandler)
  lazy val router = new Routes(httpErrorHandler, p, w, push, ps, sc, s, as, us, l, aa)
}
