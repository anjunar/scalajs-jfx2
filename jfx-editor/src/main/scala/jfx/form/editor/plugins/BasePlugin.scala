package jfx.form.editor.plugins

import jfx.form.Editor
import lexical.{EditorModules, HistoryModule, LexicalHistory, RedoModule, ToolbarElement, UndoModule}

class BasePlugin extends EditorPlugin {
  override val name: String = "base"

  override val $toolbarElements: Seq[ToolbarElement] =
    Seq(
      new UndoModule(),
      new RedoModule(),
      EditorModules.BOLD,
      EditorModules.ITALIC,
      EditorModules.UNDERLINE,
      EditorModules.STRIKETHROUGH
    )

  override val modules: Seq[lexical.EditorModule] =
    Seq(new HistoryModule(LexicalHistory.createEmptyHistoryState()))
}

object BasePlugin {
  def basePlugin(init: BasePlugin ?=> Unit = {})(using editor: Editor): BasePlugin = {
    val plugin = new BasePlugin()
    given BasePlugin = plugin
    init
    editor.registerPlugin(plugin)
    plugin
  }
}
