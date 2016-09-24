import sbt.Keys._
import sbt._

object BuildBuild extends Build {
  // "build.sbt" goes here
  override lazy val settings = super.settings ++ sbtPlugins ++ Seq(
    scalaVersion := "2.10.6",
    resolvers ++= Seq(
      "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
      "Typesafe ivy releases" at "http://repo.typesafe.com/typesafe/ivy-releases/",
      "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
      Resolver.bintrayRepo("malliina", "maven"),
      Resolver.url("malliina bintray sbt", url("https://dl.bintray.com/malliina/sbt-plugins"))(Resolver.ivyStylePatterns)
    ),
    scalacOptions ++= Seq("-unchecked", "-deprecation")
  )

  def sbtPlugins = Seq(
    "com.typesafe.play" % "sbt-plugin" % "2.5.8",
    "com.malliina" %% "sbt-play" % "0.8.2",
    "com.malliina" %% "sbt-packager" % "2.1.0",
    "com.malliina" %% "sbt-jenkins-control" % "0.3.1"
  ) map addSbtPlugin

  override lazy val projects = Seq(root)
  lazy val root = Project("plugins", file("."))
}
