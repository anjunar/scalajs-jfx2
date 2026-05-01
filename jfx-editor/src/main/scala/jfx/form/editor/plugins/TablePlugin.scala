package jfx.form.editor.plugins

import jfx.form.Editor
import lexical.{LexicalTable, RemoveTableModule, TableModule, ToolbarElement}

import scala.scalajs.js

class TablePlugin extends EditorPlugin {
  override val name: String = "table"

  override val $toolbarElements: Seq[ToolbarElement] =
    Seq(
      new TableModule(),
      new RemoveTableModule()
    )

  override val nodes: Seq[js.Any] =
    Seq(
      LexicalTable.TableNode,
      LexicalTable.TableRowNode,
      LexicalTable.TableCellNode
    )
}

object TablePlugin {
  def tablePlugin(init: TablePlugin ?=> Unit = {})(using editor: Editor): TablePlugin = {
    val plugin = new TablePlugin()
    given TablePlugin = plugin
    init
    editor.registerPlugin(plugin)
    plugin
  }
}
