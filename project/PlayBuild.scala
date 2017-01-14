import com.malliina.jenkinsctrl.models.{BuildOrder, JobName}
import com.malliina.sbt.GenericKeys._
import com.malliina.sbt.jenkinsctrl.{JenkinsKeys, JenkinsPlugin}
import com.malliina.sbt.unix.LinuxKeys.{httpPort, httpsPort}
import com.malliina.sbt.unix.LinuxPlugin
import com.malliina.sbtplay.PlayProject
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.{Keys => PackagerKeys}
import com.typesafe.sbt.web.Import.{Assets, pipelineStages}
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import play.sbt.PlayImport
import play.sbt.PlayImport.PlayKeys
import sbt.Keys._
import sbt._
import webscalajs.ScalaJSWeb
import webscalajs.WebScalaJS.autoImport.{scalaJSPipeline, scalaJSProjects}

object PlayBuild {

  lazy val frontend = Project("frontend", file("frontend"))
    .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
    .settings(
      persistLauncher := true,
      libraryDependencies ++= Seq(
        "com.lihaoyi" %%% "scalatags" % "0.6.2",
        "com.lihaoyi" %%% "upickle" % "0.4.3",
        "be.doeraene" %%% "scalajs-jquery" % "0.9.1"
//        "org.scala-js" %%% "scalajs-dom" % "0.9.1"
      )
    )

  lazy val pimpcloud = PlayProject.default("pimpcloud")
    .settings(pimpcloudSettings: _*)

  val malliinaGroup = "com.malliina"

  val pimpcloudSettings = jenkinsSettings ++ linuxSettings ++ scalaJSSettings ++ Seq(
    version := "1.6.0",
    scalaVersion := "2.11.8",
    resolvers += Resolver.bintrayRepo("malliina", "maven"),
    libraryDependencies ++= Seq(
      malliinaGroup %% "util-play" % "3.3.3",
      malliinaGroup %% "mobile-push" % "1.6.1",
      "org.java-websocket" % "Java-WebSocket" % "1.3.0",
      "com.lihaoyi" %% "scalatags" % "0.6.2",
      PlayImport.filters,
      PlayImport.cache,
      "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
    ),
    javacOptions ++= Seq(
      "-source", "1.8",
      "-target", "1.8",
      "-Xlint:-options"
    ),
    PlayKeys.externalizeResources := false,
    libs += (packageBin in Assets).value.toPath
  )

  def scalaJSSettings = Seq(
    scalaJSProjects := Seq(frontend),
    pipelineStages in Assets := Seq(scalaJSPipeline)
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
        "-Dlogger.resource=prod-logger.xml",
        "-Dfile.encoding=UTF-8",
        "-Dsun.jnu.encoding=UTF-8"
      )
    },
    PackagerKeys.packageSummary in Linux := "This is the pimpcloud summary.",
    PackagerKeys.rpmVendor := "Skogberg Labs"
  )
}
