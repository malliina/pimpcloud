import com.malliina.jenkinsctrl.models.{BuildOrder, JobName}
import com.malliina.sbt.GenericKeys._
import com.malliina.sbt.jenkinsctrl.{JenkinsKeys, JenkinsPlugin}
import com.malliina.sbt.unix.LinuxKeys.{httpPort, httpsPort}
import com.malliina.sbt.unix.LinuxPlugin
import com.malliina.sbtplay.PlayProject
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys.{maintainer, packageSummary, rpmVendor}
import com.typesafe.sbt.web.Import.{Assets, pipelineStages}
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import play.sbt.PlayImport
import play.sbt.PlayImport.PlayKeys
import sbt.Keys._
import sbt._
import sbtbuildinfo.{BuildInfoKey, BuildInfoPlugin}
import sbtbuildinfo.BuildInfoKeys.buildInfoKeys
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
    .enablePlugins(BuildInfoPlugin)
    .settings(pimpcloudSettings: _*)

  val malliinaGroup = "com.malliina"
  val utilPlayDep = malliinaGroup %% "util-play" % "3.5.3"

  val pimpcloudSettings = jenkinsSettings ++ linuxSettings ++ scalaJSSettings ++ Seq(
    buildInfoKeys += BuildInfoKey("frontName" -> (name in frontend).value),
    version := "1.6.6",
    scalaVersion := "2.11.8",
    resolvers += Resolver.bintrayRepo("malliina", "maven"),
    libraryDependencies ++= Seq(
      utilPlayDep,
      utilPlayDep % Test classifier "tests",
      malliinaGroup %% "mobile-push" % "1.7.0",
      "org.java-websocket" % "Java-WebSocket" % "1.3.0",
      PlayImport.filters,
      PlayImport.cache
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
    maintainer := "Michael Skogberg <malliina123@gmail.com>",
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
    packageSummary in Linux := "This is the pimpcloud summary.",
    rpmVendor := "Skogberg Labs"
  )
}
