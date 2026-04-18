package jfx.form.editor.plugins

import jfx.form.Editor

object PluginFactory {

  def build[T <: EditorPlugin](plugin: T)(init: T ?=> Unit = (_: T) ?=> ())(using editor: Editor): T = {
    given T = plugin
    init

    editor.addPlugin(plugin)
    plugin
  }

}
