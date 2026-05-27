package jfx.form.editor.plugins

import jfx.form.Editor
import lexical.{HorizontalRuleModule, ToolbarElement}

class HorizontalRulePlugin extends EditorPlugin {
  override val name: String = "horizontal-rule"

  override val $toolbarElements: Seq[ToolbarElement] =
    Seq(new HorizontalRuleModule())
}

object HorizontalRulePlugin {
  def horizontalRulePlugin(init: HorizontalRulePlugin ?=> Unit = {})(using editor: Editor): HorizontalRulePlugin = {
    val plugin = new HorizontalRulePlugin()
    given HorizontalRulePlugin = plugin
    init
    editor.registerPlugin(plugin)
    plugin
  }
}
