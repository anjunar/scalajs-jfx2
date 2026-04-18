package jfx.form.editor.plugins

import jfx.form.Editor
import lexical.{HeadingDropdown, LexicalRichText, ToolbarElement}

import scala.scalajs.js

class HeadingPlugin extends EditorPlugin {

  override val name: String = "heading"

  override def toolbarElements: Seq[ToolbarElement] =
    Seq(new HeadingDropdown())

  override def nodes: Seq[js.Any] =
    Seq(LexicalRichText.HeadingNode)

}

object HeadingPlugin {

  def headingPlugin(init: HeadingPlugin ?=> Unit = {})(using Editor): HeadingPlugin =
    PluginFactory.build(new HeadingPlugin())(init)

}
