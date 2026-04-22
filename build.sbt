import org.scalajs.linker.interface.{ESVersion, ModuleKind}
import org.scalajs.sbtplugin.ScalaJSPlugin

ThisBuild / version := "2.1.0"
ThisBuild / organization := "com.anjunar"
ThisBuild / organizationName := "Anjunar"
ThisBuild / organizationHomepage := Some(url("https://github.com/anjunar"))
ThisBuild / scalaVersion := "3.8.3"
ThisBuild / homepage := Some(url("https://github.com/anjunar/scalajs-jfx2"))
ThisBuild / description := "Reactive UI framework for Scala.js with lifecycle control, typed forms, routing, tables, and a composable DSL."
ThisBuild / licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/anjunar/scalajs-jfx2"),
    "scm:git:https://github.com/anjunar/scalajs-jfx2.git",
    Some("scm:git:git@github.com:anjunar/scalajs-jfx2.git")
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

lazy val commonLibrarySettings = Seq(
  Compile / doc / sources := Seq.empty,
  Compile / packageDoc / mappings += (LocalRootProject / baseDirectory).value / "README.md" -> "README.md",
  libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.8.1",
  libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.19" % Test
)

lazy val jfxCore = Project(id = "scalajs-jfx2-core", base = file("jfx-core"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "scalajs-jfx2-core",
    moduleName := "scalajs-jfx2-core",
    libraryDependencies += "com.anjunar" %%% "scala-reflect" % "1.0.0"
  )
  .settings(commonLibrarySettings)
  .settings(commonJsSettings)

lazy val jfxControls = Project(id = "scalajs-jfx2-controls", base = file("jfx-controls"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(jfxCore)
  .settings(
    name := "scalajs-jfx2-controls",
    moduleName := "scalajs-jfx2-controls"
  )
  .settings(commonLibrarySettings)
  .settings(commonJsSettings)

lazy val jfxForms = Project(id = "scalajs-jfx2-forms", base = file("jfx-forms"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(jfxCore, jfxControls)
  .settings(
    name := "scalajs-jfx2-forms",
    moduleName := "scalajs-jfx2-forms"
  )
  .settings(commonLibrarySettings)
  .settings(commonJsSettings)

lazy val jfxEditor = Project(id = "scalajs-jfx2-editor", base = file("jfx-editor"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(jfxForms)
  .settings(
    name := "scalajs-jfx2-editor",
    moduleName := "scalajs-jfx2-editor",
    libraryDependencies += "com.anjunar" %%% "scalajs-lexical" % "1.0.6"
  )
  .settings(commonLibrarySettings)
  .settings(commonJsSettings)

lazy val app = Project(id = "scalajs-jfx2-demo", base = file("application"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(jfxCore, jfxControls, jfxForms, jfxEditor)
  .settings(
    scalaJSUseMainModuleInitializer := false,
    publish / skip := true
  )
  .settings(commonJsSettings)

lazy val root = Project(id = "scalajs-jfx2-root", base = file("."))
  .aggregate(jfxCore, jfxControls, jfxForms, jfxEditor, app)
  .settings(
    publish / skip := true
  )
