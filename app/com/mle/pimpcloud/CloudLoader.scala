package com.mle.pimpcloud

import controllers._
import play.api.ApplicationLoader.Context
import play.api.mvc.EssentialFilter
import play.api.{ApplicationLoader, BuiltInComponentsFromContext, Logger}
import play.filters.gzip.GzipFilter
import router.Routes

/**
 * @author mle
 */
class CloudLoader extends ApplicationLoader {
  def load(context: Context) = {
    Logger.configure(context.environment)
    new CloudComponents(context).application
  }
}

class CloudComponents(context: Context) extends BuiltInComponentsFromContext(context) {
  override lazy val httpFilters: Seq[EssentialFilter] = Seq(new GzipFilter())

  lazy val joined = new JoinedSockets
  lazy val s = joined.servers
  lazy val ps = joined.phones
  lazy val p = new Phones(s, ps)
  lazy val sc = new ServersController(s)
  lazy val aa = new AdminAuth
  lazy val l = new Logs(aa)
  lazy val w = new Web(s)
  lazy val us = new UsageStreaming(s, p, ps, sc, aa)
  lazy val as = new Assets(httpErrorHandler)
  lazy val router = new Routes(httpErrorHandler, p, w, ps, sc, s, as, us, l, aa)
}
