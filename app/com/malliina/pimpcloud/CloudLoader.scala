package com.malliina.pimpcloud

import com.malliina.musicpimp.messaging.{ProdPusher, Pusher}
import com.malliina.play.app.DefaultApp
import com.malliina.play.controllers.AccountForms
import controllers._
import play.api.ApplicationLoader.Context
import play.api.mvc.EssentialFilter
import play.api.{BuiltInComponentsFromContext, Mode}
import play.filters.gzip.GzipFilter
import router.Routes

class CloudLoader extends DefaultApp(new CloudComponents(_, ProdPusher.fromConf))

class CloudComponents(context: Context, val pusher: Pusher) extends BuiltInComponentsFromContext(context) {
  override lazy val httpFilters: Seq[EssentialFilter] = Seq(new GzipFilter())

  // Components
  lazy val auth = new CloudAuth(materializer)
  val forms = new AccountForms
  lazy val joined = new JoinedSockets(materializer)
  lazy val s = joined.servers
  lazy val cloudAuths = joined.auths
  lazy val ps = joined.phones
  // TODO get sbt-buildinfo to provide the app name for us
  lazy val tags = CloudTags.forApp("frontend", environment.mode == Mode.Prod)
  // Controllers
  lazy val push = new Push(pusher)
  lazy val p = new Phones(tags, cloudAuths, ps, auth)
  lazy val sc = new ServersController(cloudAuths, auth)
  lazy val aa = new AdminAuth(tags, materializer)
  lazy val l = new Logs(tags, aa)
  lazy val w = new Web(tags, cloudAuths, materializer.executionContext, forms)
  lazy val us = new UsageStreaming(tags, s, p, ps, sc, aa)
  lazy val as = new Assets(httpErrorHandler)
  lazy val router = new Routes(httpErrorHandler, p, w, push, ps, sc, s, as, us, l, aa)
}
