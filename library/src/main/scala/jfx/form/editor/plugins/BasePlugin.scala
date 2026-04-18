package jfx.form.editor.plugins

import jfx.form.Editor
import lexical.{EditorModules, HistoryModule, LexicalHistory, RedoModule, ToolbarElement, UndoModule}

class BasePlugin extends EditorPlugin {

  override val name: String = "base"

  var includeHistory: Boolean = true
  var includeBold: Boolean = true
  var includeItalic: Boolean = true
  var includeUnderline: Boolean = true
  var includeStrikethrough: Boolean = true

  override def toolbarElements: Seq[ToolbarElement] =
    Seq(
      Option.when(includeHistory)(Seq(new UndoModule(), new RedoModule())).getOrElse(Seq.empty),
      Option.when(includeBold)(Seq(EditorModules.BOLD)).getOrElse(Seq.empty),
      Option.when(includeItalic)(Seq(EditorModules.ITALIC)).getOrElse(Seq.empty),
      Option.when(includeUnderline)(Seq(EditorModules.UNDERLINE)).getOrElse(Seq.empty),
      Option.when(includeStrikethrough)(Seq(EditorModules.STRIKETHROUGH)).getOrElse(Seq.empty)
    ).flatten

  override def modules: Seq[lexical.EditorModule] =
    if (includeHistory) Seq(new HistoryModule(LexicalHistory.createEmptyHistoryState()))
    else Seq.empty

}

object BasePlugin {

  def basePlugin(init: BasePlugin ?=> Unit = {})(using Editor): BasePlugin =
    PluginFactory.build(new BasePlugin())(init)

}
