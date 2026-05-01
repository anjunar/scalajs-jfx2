package jfx.form.editor.plugins

import jfx.form.Editor
import lexical.{LexicalEditor, LexicalList, ListModules, ToolbarElement}

import scala.scalajs.js

class ListPlugin extends EditorPlugin {
  override val name: String = "list"

  override val $toolbarElements: Seq[ToolbarElement] =
    Seq(ListModules.BULLET, ListModules.NUMBERED)

  override val nodes: Seq[js.Any] =
    Seq(LexicalList.ListNode, LexicalList.ListItemNode)

  override def install(editor: LexicalEditor): js.Function0[Unit] =
    LexicalList.registerList(editor)
}

object ListPlugin {
  def listPlugin(init: ListPlugin ?=> Unit = {})(using editor: Editor): ListPlugin = {
    val plugin = new ListPlugin()
    given ListPlugin = plugin
    init
    editor.registerPlugin(plugin)
    plugin
  }
}
