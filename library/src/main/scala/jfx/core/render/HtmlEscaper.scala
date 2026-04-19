package jfx.core.render

private[render] object HtmlEscaper {
  def text(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
  def attribute(value: String): String = text(value).replace("\"", "&quot;").replace("'", "&#39;")
}
