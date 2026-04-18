package jfx.form.editor.plugins

import jfx.form.Editor
import lexical.{LexicalEditor, LexicalList, ListModules, ToolbarElement}

import scala.scalajs.js

class ListPlugin extends EditorPlugin {

  override val name: String = "list"

  var includeBulletList: Boolean = true
  var includeNumberedList: Boolean = true

  override def toolbarElements: Seq[ToolbarElement] =
    Seq(
      Option.when(includeBulletList)(ListModules.BULLET),
      Option.when(includeNumberedList)(ListModules.NUMBERED)
    ).flatten

  override def nodes: Seq[js.Any] =
    Seq(LexicalList.ListNode, LexicalList.ListItemNode)

  override def install(editor: LexicalEditor): js.Function0[Unit] =
    LexicalList.registerList(editor)

}

object ListPlugin {

  def listPlugin(init: ListPlugin ?=> Unit = {})(using Editor): ListPlugin =
    PluginFactory.build(new ListPlugin())(init)

}
