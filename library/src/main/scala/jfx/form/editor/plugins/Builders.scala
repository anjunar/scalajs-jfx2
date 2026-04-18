package jfx.form.editor.plugins

import jfx.form.Editor

def basePlugin(init: BasePlugin ?=> Unit = {})(using Editor): BasePlugin =
  BasePlugin.basePlugin(init)

def headingPlugin(init: HeadingPlugin ?=> Unit = {})(using Editor): HeadingPlugin =
  HeadingPlugin.headingPlugin(init)

def listPlugin(init: ListPlugin ?=> Unit = {})(using Editor): ListPlugin =
  ListPlugin.listPlugin(init)

def linkPlugin(init: LinkPlugin ?=> Unit = {})(using Editor): LinkPlugin =
  LinkPlugin.linkPlugin(init)

def codePlugin(init: CodePlugin ?=> Unit = {})(using Editor): CodePlugin =
  CodePlugin.codePlugin(init)

def defaultPlugins()(using Editor): Seq[EditorPlugin] =
  Seq(
    basePlugin(),
    headingPlugin(),
    listPlugin(),
    linkPlugin(),
    codePlugin()
  )
