import com.mle.sbt.unix.LinuxPlugin
import com.mle.sbtplay.PlayProjects
import com.typesafe.sbt.SbtNativePackager
import sbt.Keys._
import sbt._

object PlayBuild extends Build {
  lazy val p = PlayProjects.plainPlayProject("pimpcloud").settings(commonSettings: _*)
  val mleGroup = "com.github.malliina"
  val commonSettings = SbtNativePackager.packagerSettings ++ LinuxPlugin.debianSettings ++ Seq(
    version := "0.1.9",
    scalaVersion := "2.11.6",
    retrieveManaged := false,
    fork in Test := true,
    parallelExecution in Test := false,
    resolvers ++= Seq(
      Resolver.bintrayRepo("malliina", "maven")
    ),
    libraryDependencies ++= Seq(
      mleGroup %% "play-base" % "0.4.0",
      "org.java-websocket" % "Java-WebSocket" % "1.3.0",
      play.PlayImport.filters,
      play.PlayImport.cache
    ),
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7", "-Xlint:-options"),
    scalacOptions += "-target:jvm-1.7"
  )

}

