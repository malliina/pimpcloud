import com.malliina.jenkinsctrl.models.{JobName, BuildOrder}
import com.malliina.sbt.GenericKeys._
import com.malliina.sbt.jenkinsctrl.{JenkinsKeys, JenkinsPlugin}
import com.malliina.sbt.unix.LinuxKeys.{httpPort, httpsPort}
import com.malliina.sbt.unix.LinuxPlugin
import com.malliina.sbtplay.PlayProject
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.{Keys => PackagerKeys}
import play.routes.compiler.InjectedRoutesGenerator
import play.sbt.PlayImport
import play.sbt.PlayImport.PlayKeys
import play.sbt.routes.RoutesKeys
import sbt.Keys._
import sbt._

object PlayBuild extends Build {

  lazy val p = PlayProject("pimpcloud").settings(commonSettings: _*)

  val malliinaGroup = "com.malliina"

  val commonSettings = JenkinsPlugin.settings ++ linuxSettings ++ Seq(
    version := "0.6.0",
    scalaVersion := "2.11.7",
    retrieveManaged := false,
    fork in Test := true,
    parallelExecution in Test := false,
    resolvers ++= Seq(
      Resolver.bintrayRepo("malliina", "maven")
    ),
    libraryDependencies ++= Seq(
      malliinaGroup %% "play-base" % "2.5.0",
      malliinaGroup %% "mobile-push" % "1.3.1",
      "org.java-websocket" % "Java-WebSocket" % "1.3.0",
      PlayImport.filters,
      PlayImport.cache,
      PlayImport.specs2 % Test,
      "org.scalatest" %% "scalatest" % "2.2.5" % Test
    ),
    javacOptions ++= Seq(
      "-source", "1.8",
      "-target", "1.8",
      "-Xlint:-options"
    ),
    scalacOptions += "-target:jvm-1.8",
    RoutesKeys.routesGenerator := InjectedRoutesGenerator,
    PlayKeys.externalizeResources := false
  )

  def jenkinsSettings = JenkinsPlugin.settings ++ Seq(
    JenkinsKeys.jenkinsDefaultBuild := Option(BuildOrder.simple(JobName("pimpcloud")))
  )

  def linuxSettings = LinuxPlugin.playSettings ++ Seq(
    httpPort in Linux := Option("disabled"),
    httpsPort in Linux := Option("8457"),
    PackagerKeys.maintainer := "Michael Skogberg <malliina123@gmail.com>",
    manufacturer := "Skogberg Labs",
    mainClass := Some("com.malliina.pimpcloud.Starter"),
    javaOptions in Universal ++= {
      val linuxName = (name in Linux).value
      Seq(
        s"-Dgoogle.oauth=/etc/$linuxName/google-oauth.key",
        s"-Dpush.conf=/etc/$linuxName/push.conf",
        s"-Dlog.dir=/var/run/$linuxName/logs",
        "-Dlogger.resource=prod-logger.xml"
      )
    },
    PackagerKeys.packageSummary in Linux := "This is the pimpcloud summary.",
    PackagerKeys.rpmVendor := "Skogberg Labs"
  )
}
