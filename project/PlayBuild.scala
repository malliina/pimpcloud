import com.mle.sbt.GenericKeys._
import com.mle.sbt.unix.LinuxKeys.{httpPort, httpsPort}
import com.mle.sbt.unix.LinuxPlugin
import com.mle.sbtplay.PlayProjects
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.{Keys => PackagerKeys}
import play.sbt.PlayImport
import sbt.Keys._
import sbt._

object PlayBuild extends Build {
  lazy val p = PlayProjects.plainPlayProject("pimpcloud").settings(commonSettings: _*)
  val mleGroup = "com.github.malliina"
  val commonSettings = linuxSettings ++ Seq(
    version := "0.4.0",
    scalaVersion := "2.11.7",
//    exportJars := true,
    retrieveManaged := false,
    fork in Test := true,
    parallelExecution in Test := false,
    resolvers ++= Seq(
      Resolver.bintrayRepo("malliina", "maven")
    ),
    libraryDependencies ++= Seq(
      mleGroup %% "play-base" % "0.6.1",
      "org.java-websocket" % "Java-WebSocket" % "1.3.0",
      PlayImport.filters,
      PlayImport.cache
    ),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:-options"),
    scalacOptions += "-target:jvm-1.8"
  )

  def linuxSettings = LinuxPlugin.playSettings ++ Seq(
    httpPort in Linux := Option("disabled"),
    httpsPort in Linux := Option("8457"),
    PackagerKeys.maintainer := "Michael Skogberg <malliina123@gmail.com>",
    manufacturer := "Skogberg Labs",
    mainClass := Some("com.mle.pimpcloud.Starter"),
    javaOptions in Universal += {
      val linuxName = (name in Linux).value
      s"-Dgoogle.oauth=/etc/$linuxName/google-oauth.key"
    },
    PackagerKeys.packageSummary in Linux := "This is the pimpcloud summary.",
    PackagerKeys.rpmVendor := "Skogberg Labs"
  )
}
