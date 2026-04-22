package jfx.form.editor.plugins

import jfx.form.Editor

def basePlugin(init: BasePlugin ?=> Unit = {})(using editor: Editor): BasePlugin =
  BasePlugin.basePlugin(init)

def headingPlugin(init: HeadingPlugin ?=> Unit = {})(using editor: Editor): HeadingPlugin =
  HeadingPlugin.headingPlugin(init)

def listPlugin(init: ListPlugin ?=> Unit = {})(using editor: Editor): ListPlugin =
  ListPlugin.listPlugin(init)

def tablePlugin(init: TablePlugin ?=> Unit = {})(using editor: Editor): TablePlugin =
  TablePlugin.tablePlugin(init)

def linkPlugin(init: LinkPlugin ?=> Unit = {})(using editor: Editor): LinkPlugin =
  LinkPlugin.linkPlugin(init)

def imagePlugin(init: ImagePlugin ?=> Unit = {})(using editor: Editor): ImagePlugin =
  ImagePlugin.imagePlugin(init)

def codePlugin(init: CodePlugin ?=> Unit = {})(using editor: Editor): CodePlugin =
  CodePlugin.codePlugin(init)
