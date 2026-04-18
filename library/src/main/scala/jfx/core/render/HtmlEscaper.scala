package jfx.core.render

private[render] object HtmlEscaper {

  def text(value: String): String =
    if (value == null) ""
    else escape(value, escapeQuotes = false)

  def attribute(value: String): String =
    if (value == null) ""
    else escape(value, escapeQuotes = true)

  private def escape(value: String, escapeQuotes: Boolean): String = {
    val builder = new StringBuilder(value.length)

    value.foreach {
      case '&' => builder.append("&amp;")
      case '<' => builder.append("&lt;")
      case '>' => builder.append("&gt;")
      case '"' if escapeQuotes => builder.append("&quot;")
      case '\'' if escapeQuotes => builder.append("&#39;")
      case char => builder.append(char)
    }

    builder.toString()
  }

}
