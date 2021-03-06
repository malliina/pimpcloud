package com.malliina.pimpcloud

import buildinfo.BuildInfo
import com.malliina.musicpimp.messaging.{ProdPusher, Pusher}
import com.malliina.oauth.{GoogleOAuthCredentials, GoogleOAuthReader}
import com.malliina.play.app.DefaultApp
import com.malliina.play.controllers.AccountForms
import controllers._
import play.api.ApplicationLoader.Context
import play.api.mvc.EssentialFilter
import play.api.{BuiltInComponentsFromContext, Mode}
import play.filters.gzip.GzipFilter
import router.Routes

class CloudLoader extends DefaultApp(new ProdComponents(_))

class ProdComponents(context: Context) extends CloudComponents(context, ProdPusher.fromConf, GoogleOAuthReader.load) {
  override lazy val prodAuth: PimpAuth = new ProdAuth(new OAuthCtrl(adminAuth))
}

abstract class CloudComponents(context: Context,
                               pusher: Pusher,
                               oauthCreds: GoogleOAuthCredentials) extends BuiltInComponentsFromContext(context) {
  val adminAuth = new AdminOAuth(oauthCreds, materializer)

  def prodAuth: PimpAuth

  // Components
  override lazy val httpFilters: Seq[EssentialFilter] = Seq(new GzipFilter())
  lazy val auth = new CloudAuth(materializer)
  val forms = new AccountForms
  lazy val joined = new JoinedSockets(materializer)
  lazy val s = joined.servers
  lazy val cloudAuths = joined.auths
  lazy val ps = joined.phones
  lazy val tags = CloudTags.forApp(BuildInfo.frontName, environment.mode == Mode.Prod)

  // Controllers
  lazy val push = new Push(pusher)
  lazy val p = new Phones(tags, cloudAuths, ps, auth)
  lazy val sc = new ServersController(cloudAuths, auth)
  lazy val aa = new AdminAuth(prodAuth, adminAuth, tags, materializer)
  lazy val l = new Logs(tags, prodAuth, materializer)
  lazy val w = new Web(tags, cloudAuths, materializer.executionContext, forms)
  lazy val us = new UsageStreaming(s, ps, prodAuth)
  lazy val as = new Assets(httpErrorHandler)
  lazy val router = new Routes(httpErrorHandler, p, w, push, ps, sc, s, as, l, adminAuth, aa, us)
}
