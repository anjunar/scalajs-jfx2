package jfx.form.editor.plugins

import jfx.form.Editor
import lexical.ToolbarElement
import lexical.codemirror.{CodeMirrorModule, CodeMirrorNode}

import scala.scalajs.js

class CodePlugin extends EditorPlugin {

  override val name: String = "code"

  override def toolbarElements: Seq[ToolbarElement] =
    Seq(new CodeMirrorModule())

  override def nodes: Seq[js.Any] =
    Seq(js.constructorOf[CodeMirrorNode])

}

object CodePlugin {

  def codePlugin(init: CodePlugin ?=> Unit = {})(using Editor): CodePlugin =
    PluginFactory.build(new CodePlugin())(init)

}
