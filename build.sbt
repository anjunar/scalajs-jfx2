import org.scalajs.linker.interface.{ESVersion, ModuleKind}
import org.scalajs.sbtplugin.ScalaJSPlugin

version := "2.3.1-SNAPSHOT"
organization := "com.anjunar"
organizationName := "Anjunar"
organizationHomepage := Some(url("https://github.com/anjunar"))

scalaVersion := "3.3.8"

homepage := Some(url("https://github.com/anjunar/scalajs-jfx2"))
description := "Reactive UI framework for Scala.js with lifecycle control, typed forms, routing, tables, and a composable DSL."

licenses := Seq(License.MIT)

scmInfo := Some(
  ScmInfo(
    url("https://github.com/anjunar/scalajs-jfx2"),
    "scm:git:https://github.com/anjunar/scalajs-jfx2.git",
    Some("scm:git:git@github.com:anjunar/scalajs-jfx2.git")
  )
)

developers := List(
  Developer(
    id = "anjunar",
    name = "Patrick Bittner",
    email = "anjunar@gmx.de",
    url = url("https://github.com/anjunar")
  )
)

versionScheme := Some("early-semver")

pomIncludeRepository := { _ => false }
publishMavenStyle := true

publishTo := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if version.value.endsWith("-SNAPSHOT") then
    Some("central-snapshots" at centralSnapshots)
  else
    localStaging.value
}

lazy val commonJsSettings = Seq(
  scalaJSLinkerConfig ~= (
    _.withModuleKind(ModuleKind.ESModule)
      .withESFeatures(_.withESVersion(ESVersion.ES2021))
    )
)

lazy val commonLibrarySettings = Seq(
  Compile / doc / sources := Seq.empty,
  Compile / packageDoc / mappings += {
    val converter = fileConverter.value
    val readme = ((LocalRootProject / baseDirectory).value / "README.md").toPath
    converter.toVirtualFile(readme) -> "README.md"
  },
  libraryDependencies += "org.scala-js" %% "scalajs-dom" % "2.8.1",
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test
)

lazy val jfxCore = Project(id = "scalajs-jfx2-core", base = file("jfx-core"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "scalajs-jfx2-core",
    moduleName := "scalajs-jfx2-core",
    libraryDependencies += "com.anjunar" %% "scala-reflect" % "1.1.3"
  )
  .settings(commonLibrarySettings)
  .settings(commonJsSettings)

lazy val jfxRouter = Project(id = "scalajs-jfx2-router", base = file("jfx-router"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(jfxCore)
  .settings(
    name := "scalajs-jfx2-router",
    moduleName := "scalajs-jfx2-router"
  )
  .settings(commonLibrarySettings)
  .settings(commonJsSettings)

lazy val jfxViewport = Project(id = "scalajs-jfx2-viewport", base = file("jfx-viewport"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(jfxCore)
  .settings(
    name := "scalajs-jfx2-viewport",
    moduleName := "scalajs-jfx2-viewport"
  )
  .settings(commonLibrarySettings)
  .settings(commonJsSettings)

lazy val jfxI18n = Project(id = "scalajs-jfx2-i18n", base = file("jfx-i18n"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(jfxCore)
  .settings(
    name := "scalajs-jfx2-i18n",
    moduleName := "scalajs-jfx2-i18n"
  )
  .settings(commonLibrarySettings)
  .settings(commonJsSettings)

lazy val jfxJson = Project(id = "scalajs-jfx2-json", base = file("jfx-json"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(jfxCore)
  .settings(
    name := "scalajs-jfx2-json",
    moduleName := "scalajs-jfx2-json",
    libraryDependencies += "com.anjunar" %% "scala-reflect" % "1.1.3"
  )
  .settings(commonLibrarySettings)
  .settings(commonJsSettings)

lazy val jfxControls = Project(id = "scalajs-jfx2-controls", base = file("jfx-controls"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(jfxCore, jfxRouter, jfxViewport % "test->compile")
  .settings(
    name := "scalajs-jfx2-controls",
    moduleName := "scalajs-jfx2-controls"
  )
  .settings(commonLibrarySettings)
  .settings(commonJsSettings)

lazy val jfxForms = Project(id = "scalajs-jfx2-forms", base = file("jfx-forms"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(jfxCore, jfxControls, jfxViewport)
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
    libraryDependencies += "com.anjunar" %% "scalajs-lexical" % "1.3.0"
  )
  .settings(commonLibrarySettings)
  .settings(commonJsSettings)

lazy val jfxWebAuthn = Project(id = "scalajs-jfx2-webauthn", base = file("jfx-webAuthn"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(jfxCore)
  .settings(
    name := "scalajs-jfx2-webauthn",
    moduleName := "scalajs-jfx2-webauthn"
  )
  .settings(commonLibrarySettings)
  .settings(commonJsSettings)

lazy val jfxSsr = Project(id = "scalajs-jfx2-ssr", base = file("jfx-ssr"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(jfxCore)
  .settings(
    name := "scalajs-jfx2-ssr",
    moduleName := "scalajs-jfx2-ssr"
  )
  .settings(commonLibrarySettings)
  .settings(commonJsSettings)

lazy val app = Project(id = "scalajs-jfx2-demo", base = file("application"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(
    jfxCore,
    jfxRouter,
    jfxViewport,
    jfxI18n,
    jfxJson,
    jfxControls,
    jfxForms,
    jfxEditor,
    jfxWebAuthn,
    jfxSsr
  )
  .settings(
    scalaJSUseMainModuleInitializer := false,
    publish / skip := true
  )
  .settings(commonJsSettings)

lazy val root = Project(id = "scalajs-jfx2-root", base = file("."))
  .aggregate(
    jfxCore,
    jfxRouter,
    jfxViewport,
    jfxI18n,
    jfxJson,
    jfxControls,
    jfxForms,
    jfxEditor,
    jfxWebAuthn,
    jfxSsr,
    app
  )
  .settings(
    publish / skip := true
  )