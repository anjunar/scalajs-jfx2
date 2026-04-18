package jfx.form.editor

import scala.collection.mutable

object SsrCodeHighlighter {

  final case class Token(value: String, className: String = "")

  private val languageKeywords: Map[String, Set[String]] =
    Map(
      "scala" -> Set("abstract", "case", "catch", "class", "def", "do", "else", "enum", "export", "extends", "false", "final", "finally", "for", "given", "if", "implicit", "import", "lazy", "match", "new", "null", "object", "opaque", "override", "package", "private", "protected", "return", "sealed", "super", "then", "throw", "trait", "true", "try", "type", "val", "var", "while", "with", "yield"),
      "java" -> Set("abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "false", "final", "finally", "float", "for", "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "null", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try", "void", "volatile", "while"),
      "javascript" -> Set("async", "await", "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete", "do", "else", "export", "extends", "false", "finally", "for", "from", "function", "if", "import", "in", "instanceof", "let", "new", "null", "of", "return", "static", "super", "switch", "this", "throw", "true", "try", "typeof", "undefined", "var", "void", "while", "with", "yield"),
      "typescript" -> Set("abstract", "any", "as", "async", "await", "boolean", "break", "case", "catch", "class", "const", "continue", "declare", "default", "delete", "do", "else", "enum", "export", "extends", "false", "finally", "for", "from", "function", "if", "implements", "import", "in", "infer", "instanceof", "interface", "keyof", "let", "module", "namespace", "never", "new", "null", "number", "of", "private", "protected", "public", "readonly", "return", "static", "string", "super", "switch", "this", "throw", "true", "try", "type", "typeof", "undefined", "unknown", "var", "void", "while", "with", "yield"),
      "python" -> Set("and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del", "elif", "else", "except", "False", "finally", "for", "from", "global", "if", "import", "in", "is", "lambda", "None", "nonlocal", "not", "or", "pass", "raise", "return", "True", "try", "while", "with", "yield"),
      "rust" -> Set("as", "async", "await", "break", "const", "continue", "crate", "dyn", "else", "enum", "extern", "false", "fn", "for", "if", "impl", "in", "let", "loop", "match", "mod", "move", "mut", "pub", "ref", "return", "self", "Self", "static", "struct", "super", "trait", "true", "type", "unsafe", "use", "where", "while"),
      "sql" -> Set("ADD", "ALL", "ALTER", "AND", "AS", "ASC", "BETWEEN", "BY", "CASE", "CREATE", "DELETE", "DESC", "DISTINCT", "DROP", "ELSE", "END", "FROM", "GROUP", "HAVING", "IN", "INNER", "INSERT", "INTO", "IS", "JOIN", "LEFT", "LIKE", "LIMIT", "NOT", "NULL", "ON", "OR", "ORDER", "OUTER", "RIGHT", "SELECT", "SET", "TABLE", "THEN", "UPDATE", "VALUES", "WHEN", "WHERE")
    )

  def highlight(code: String, language: String): Vector[Token] = {
    val normalizedLanguage = Option(language).getOrElse("").toLowerCase

    normalizedLanguage match {
      case "html" | "xml" =>
        highlightMarkup(code)
      case "css" =>
        highlightCss(code)
      case _ =>
        highlightGeneric(code, languageKeywords.getOrElse(normalizedLanguage, Set.empty))
    }
  }

  private def highlightGeneric(code: String, keywords: Set[String]): Vector[Token] = {
    val tokens = mutable.ArrayBuffer.empty[Token]
    var index = 0

    def append(value: String, className: String = ""): Unit =
      if (value.nonEmpty) tokens += Token(value, className)

    while (index < code.length) {
      val char = code.charAt(index)
      val next = if (index + 1 < code.length) code.charAt(index + 1) else 0.toChar

      if (char.isWhitespace) {
        val start = index
        while (index < code.length && code.charAt(index).isWhitespace) index += 1
        append(code.substring(start, index))
      } else if (char == '/' && next == '/') {
        val start = index
        index = consumeUntilLineEnd(code, index + 2)
        append(code.substring(start, index), "hljs-comment")
      } else if (char == '/' && next == '*') {
        val start = index
        index = consumeUntil(code, index + 2, "*/")
        append(code.substring(start, index), "hljs-comment")
      } else if (char == '#') {
        val start = index
        index = consumeUntilLineEnd(code, index + 1)
        append(code.substring(start, index), "hljs-comment")
      } else if (char == '"' || char == '\'' || char == '`') {
        val start = index
        index = consumeString(code, index, char)
        append(code.substring(start, index), "hljs-string")
      } else if (char.isDigit) {
        val start = index
        while (index < code.length && isNumberPart(code.charAt(index))) index += 1
        append(code.substring(start, index), "hljs-number")
      } else if (isIdentifierStart(char)) {
        val start = index
        while (index < code.length && isIdentifierPart(code.charAt(index))) index += 1
        val word = code.substring(start, index)
        val className =
          if (keywords.contains(word) || keywords.contains(word.toUpperCase)) "hljs-keyword"
          else ""
        append(word, className)
      } else {
        append(char.toString)
        index += 1
      }
    }

    tokens.toVector
  }

  private def highlightMarkup(code: String): Vector[Token] = {
    val tokens = mutable.ArrayBuffer.empty[Token]
    var index = 0

    def append(value: String, className: String = ""): Unit =
      if (value.nonEmpty) tokens += Token(value, className)

    while (index < code.length) {
      if (code.startsWith("<!--", index)) {
        val start = index
        index = consumeUntil(code, index + 4, "-->")
        append(code.substring(start, index), "hljs-comment")
      } else if (code.charAt(index) == '<') {
        append("<", "hljs-tag")
        index += 1

        if (index < code.length && code.charAt(index) == '/') {
          append("/", "hljs-tag")
          index += 1
        }

        val tagStart = index
        while (index < code.length && isIdentifierPart(code.charAt(index))) index += 1
        append(code.substring(tagStart, index), "hljs-name")

        while (index < code.length && code.charAt(index) != '>') {
          val char = code.charAt(index)
          if (char.isWhitespace) {
            val start = index
            while (index < code.length && code.charAt(index).isWhitespace) index += 1
            append(code.substring(start, index))
          } else if (char == '"' || char == '\'') {
            val start = index
            index = consumeString(code, index, char)
            append(code.substring(start, index), "hljs-string")
          } else if (isIdentifierStart(char)) {
            val start = index
            while (index < code.length && isIdentifierPart(code.charAt(index))) index += 1
            append(code.substring(start, index), "hljs-attr")
          } else {
            append(char.toString, "hljs-tag")
            index += 1
          }
        }

        if (index < code.length) {
          append(">", "hljs-tag")
          index += 1
        }
      } else {
        val start = index
        while (index < code.length && code.charAt(index) != '<') index += 1
        append(code.substring(start, index))
      }
    }

    tokens.toVector
  }

  private def highlightCss(code: String): Vector[Token] = {
    val keywords = Set("align-items", "background", "border", "color", "display", "flex", "font", "gap", "grid", "height", "justify-content", "margin", "padding", "position", "width")
    highlightGeneric(code, keywords).map {
      case Token(value, "hljs-keyword") => Token(value, "hljs-attribute")
      case other => other
    }
  }

  private def consumeUntilLineEnd(code: String, from: Int): Int = {
    var index = from
    while (index < code.length && code.charAt(index) != '\n') index += 1
    index
  }

  private def consumeUntil(code: String, from: Int, marker: String): Int = {
    val found = code.indexOf(marker, from)
    if (found < 0) code.length else found + marker.length
  }

  private def consumeString(code: String, from: Int, delimiter: Char): Int = {
    var index = from + 1
    var escaped = false

    while (index < code.length) {
      val char = code.charAt(index)
      if (escaped) {
        escaped = false
      } else if (char == '\\') {
        escaped = true
      } else if (char == delimiter) {
        return index + 1
      }

      index += 1
    }

    index
  }

  private def isIdentifierStart(char: Char): Boolean =
    char.isLetter || char == '_' || char == '$'

  private def isIdentifierPart(char: Char): Boolean =
    char.isLetterOrDigit || char == '_' || char == '-' || char == '$'

  private def isNumberPart(char: Char): Boolean =
    char.isLetterOrDigit || char == '.' || char == '_' || char == 'x' || char == 'X'

}
