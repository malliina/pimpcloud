import com.mle.sbt.GenericKeys._
import com.mle.sbt.GenericPlugin
import com.mle.sbtplay.PlayProjects
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.{Keys => PackagerKeys}
import sbt.Keys._
import sbt._

object PlayBuild extends Build {
  lazy val p = PlayProjects.plainPlayProject("pimpcloud").settings(commonSettings: _*)
  val mleGroup = "com.github.malliina"
  val commonSettings = linuxSettings ++ Seq(
    version := "0.2.3",
    scalaVersion := "2.11.7",
    exportJars := true,
    retrieveManaged := false,
    fork in Test := true,
    parallelExecution in Test := false,
    resolvers ++= Seq(
      Resolver.bintrayRepo("malliina", "maven")
    ),
    libraryDependencies ++= Seq(
      mleGroup %% "play-base" % "0.5.1",
      "org.java-websocket" % "Java-WebSocket" % "1.3.0",
      play.sbt.PlayImport.filters,
      play.sbt.PlayImport.cache
    ),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:-options"),
    scalacOptions += "-target:jvm-1.8"
  )

  def linuxSettings = GenericPlugin.genericSettings ++ GenericPlugin.confSettings ++ Seq(
    PackagerKeys.maintainer := "Michael Skogberg <malliina123@gmail.com>",
    manufacturer := "Skogberg Labs",
    mainClass := Some("com.mle.pimpcloud.Starter"),
    javaOptions in Universal ++= {
      val linuxName = (name in Linux).value
      Seq(
        "-Dhttp.port=8456",
        s"-Dpidfile.path=/var/run/$linuxName/$linuxName.pid",
        s"-D$linuxName.home=/var/run/$linuxName",
        s"-Dgoogle.oauth=/etc/$linuxName/google-oauth.key"
      )
    },
    PackagerKeys.packageSummary in Linux := "pimpcloud summary here.",
    PackagerKeys.rpmVendor := "Skogberg Labs"
  )
}
