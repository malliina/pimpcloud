import com.mle.sbt.unix.LinuxPlugin
import com.mle.sbtplay.PlayProjects
import com.typesafe.sbt.SbtNativePackager
import sbt.Keys._
import sbt._

object PlayBuild extends Build {

  lazy val p = PlayProjects.playProject("pimpcloud").settings(commonSettings: _*)
  val mleGroup = "com.github.malliina"
  val commonSettings = SbtNativePackager.packagerSettings ++ LinuxPlugin.debianSettings ++ Seq(
    version := "0.0.4",
    scalaVersion := "2.11.2",
    retrieveManaged := false,
    fork in Test := true,
    libraryDependencies ++= Seq(
      mleGroup %% "util-play" % "1.5.8",
      mleGroup %% "play-base" % "0.1.0",
      "org.java-websocket" % "Java-WebSocket" % "1.3.0",
      "com.h2database" % "h2" % "1.4.181",
      "com.typesafe.slick" %% "slick" % "2.1.0",
      play.PlayImport.filters
    ),
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
    scalacOptions += "-target:jvm-1.7"
  )
}