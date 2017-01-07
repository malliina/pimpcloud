import sbt.Keys._
import sbt._

object BuildBuild {
  // "build.sbt" goes here
  lazy val settings = sbtPlugins ++ Seq(
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
    "com.malliina" %% "sbt-play" % "0.9.1",
    "com.malliina" %% "sbt-packager" % "2.1.0",
    "com.malliina" %% "sbt-jenkins-control" % "0.3.1",
    "org.scala-js" % "sbt-scalajs" % "0.6.13",
    "com.vmunier" % "sbt-web-scalajs" % "1.0.3"
  ) map addSbtPlugin
}
