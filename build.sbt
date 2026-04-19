import org.scalajs.linker.interface.{ESVersion, ModuleKind}
import org.scalajs.sbtplugin.ScalaJSPlugin

ThisBuild / version := "1.0.0"
ThisBuild / organization := "com.anjunar"
ThisBuild / organizationName := "Anjunar"
ThisBuild / organizationHomepage := Some(url("https://github.com/anjunar"))
ThisBuild / scalaVersion := "3.8.3"
ThisBuild / homepage := Some(url("https://github.com/anjunar/scalajs-jfx"))
ThisBuild / description := "Reactive UI framework for Scala.js with lifecycle control, typed forms, routing, tables, and a composable DSL."
ThisBuild / licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/anjunar/scalajs-jfx"),
    "scm:git:https://github.com/anjunar/scalajs-jfx.git",
    Some("scm:git:git@github.com:anjunar/scalajs-jfx.git")
  )
)
ThisBuild / developers := List(
  Developer(
    id = "anjunar",
    name = "Patrick Bittner",
    email = "anjunar@gmx.de",
    url = url("https://github.com/anjunar")
  )
)
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishMavenStyle := true
ThisBuild / publishTo := {
  if (isSnapshot.value) {
    Some("central-snapshots" at "https://central.sonatype.com/repository/maven-snapshots/")
  } else {
    localStaging.value
  }
}

lazy val commonJsSettings = Seq(
  scalaJSLinkerConfig ~= (
    _.withModuleKind(ModuleKind.ESModule)
      .withESFeatures(_.withESVersion(ESVersion.ES2021))
    )
)

lazy val jfx = Project(id = "scalajs-jfx2", base = file("library"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "scalajs-jfx2",
    moduleName := "scalajs-jfx2",
    Compile / doc / sources := Seq.empty,
    libraryDependencies += "com.anjunar" %%% "scalajs-lexical" % "1.0.6",
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.8.1",
    libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.19" % Test
  )
  .settings(commonJsSettings)

lazy val app = Project(id = "scalajs-jfx2-demo", base = file("application"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(jfx)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    publish / skip := true
  )
  .settings(commonJsSettings)

lazy val root = Project(id = "scalajs-jfx2-root", base = file("."))
  .aggregate(jfx, app)
  .settings(
    publish / skip := true
  )
