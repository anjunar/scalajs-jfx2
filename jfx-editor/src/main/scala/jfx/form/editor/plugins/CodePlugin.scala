package jfx.form.editor.plugins

import jfx.form.Editor
import lexical.ToolbarElement
import lexical.codemirror.{CodeMirrorModule, CodeMirrorNode, CodeMirrorPlugin}

import scala.scalajs.js

class CodePlugin extends EditorPlugin {
  override val name: String = "code"

  override val $toolbarElements: Seq[ToolbarElement] =
    Seq(new CodeMirrorModule())

  override val nodes: Seq[js.Any] =
    Seq(js.constructorOf[CodeMirrorNode])

  override def install(editor: lexical.LexicalEditor): js.Function0[Unit] =
    CodeMirrorPlugin.register(editor)
}

object CodePlugin {
  def codePlugin(init: CodePlugin ?=> Unit = {})(using editor: Editor): CodePlugin = {
    val plugin = new CodePlugin()
    given CodePlugin = plugin
    init
    editor.registerPlugin(plugin)
    plugin
  }
}
