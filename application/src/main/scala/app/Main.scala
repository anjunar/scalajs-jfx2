package app

import jfx.control.TableView
import jfx.ssr.Ssr
import jfx.dsl.DslRuntime

object Main {
  def main(args: Array[String]): Unit = {
    val html = Ssr.renderToString {
      val tv = new TableView[String]()
      tv.items += "Item A"
      tv.items += "Item B"
      DslRuntime.attach(tv)
      tv
    }
    
    println("RENDERED HTML:")
    println(html)
  }
}
