package jfx.control

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import jfx.ssr.Ssr
import jfx.router.Route.{asyncRoute, page}
import jfx.router.Router.router
import jfx.control.TableView.*
import jfx.control.TableColumn.*
import jfx.core.component.Component.*

class TableViewSsrSpec extends AnyFlatSpec with Matchers {

  "TableView SSR" should "render specific items based on query params" in {
    val members = (0 until 20).map(i => s"Member $i")
    
    val html = Ssr.renderToString {
      router(Seq(
        asyncRoute("/") {
          page {
            tableView[String] {
              crawlable = true
              items = members
              column[String, String]("Name") { item =>
                text = item
              }
            }
          }
        }
      ), "/?offset=5&limit=5")
    }
    
    // Wir geben das HTML aus, um es manuell zu prüfen
    println("DEBUG SSR HTML OUTPUT:")
    println(html)
    
    // Prüfen, ob die Items im Offset-Bereich vorhanden sind
    html should not include "Member 4"
    html should include ("Member 5")
    html should include ("Member 9")
    html should not include "Member 10"
    
    // Der "More items" Link sollte auf die nächste Seite zeigen
    html should include ("href=\"?offset=10&amp;limit=5\"")
    html should include ("More items...")
  }
}
