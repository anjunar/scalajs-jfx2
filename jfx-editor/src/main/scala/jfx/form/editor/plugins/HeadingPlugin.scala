package jfx.form.editor.plugins

import jfx.form.Editor
import lexical.{HeadingDropdown, LexicalRichText, ToolbarElement}

import scala.scalajs.js

class HeadingPlugin extends EditorPlugin {
  override val name: String = "heading"

  override val toolbarElements: Seq[ToolbarElement] =
    Seq(new HeadingDropdown())

  override val nodes: Seq[js.Any] =
    Seq(LexicalRichText.HeadingNode)
}

object HeadingPlugin {
  def headingPlugin(init: HeadingPlugin ?=> Unit = {})(using editor: Editor): HeadingPlugin = {
    val plugin = new HeadingPlugin()
    given HeadingPlugin = plugin
    init
    editor.registerPlugin(plugin)
    plugin
  }
}
