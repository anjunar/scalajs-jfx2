package jfx.form.editor.plugins

import jfx.form.Editor

def basePlugin(init: BasePlugin ?=> Unit = {})(using editor: Editor): BasePlugin =
  BasePlugin.basePlugin(init)
