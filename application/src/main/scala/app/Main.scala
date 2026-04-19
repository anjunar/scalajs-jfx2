package app

import jfx.action.Button.*
import jfx.control.TableView
import jfx.control.TableColumn.column
import jfx.core.component.Box.*
import jfx.core.state.{ListProperty, Property}
import jfx.dsl.DslRuntime
import jfx.form.Input.*
import jfx.hydration.Hydration
import jfx.layout.Div.div
import jfx.layout.VBox.vbox
import jfx.layout.HBox.hbox
import jfx.ssr.Ssr
import jfx.statement.Condition.condition
import jfx.statement.ForEach.forEach
import org.scalajs.dom
import scala.scalajs.js.annotation.JSExportTopLevel

object Main {

  def main(args: Array[String]): Unit = {
    boot()
  }

  @JSExportTopLevel("boot")
  def boot(): Unit = {
    val root = dom.document.getElementById("root")
    if (root != null && root.children.length > 0) {
      println("Existing content found, starting hydration...")
      Hydration.hydrate(root) {
        demo()
      }
    } else if (root != null) {
      val backend = jfx.core.render.BrowserRenderBackend
      val cursor = backend.nextCursor(Some(new jfx.core.render.DomHostElement("div", root)))
      jfx.core.render.RenderBackend.withBackend(backend) {
        DslRuntime.withCursor(cursor) {
          demo()
        }
      }
    }
  }

  private def demo() = {
    val name = Property("Ada")
    val clicks = Property(0)
    val milestones = ListProperty[String]()
    milestones += "SSR rendert ohne Browser-DOM"
    milestones += "Hydration claimed bestehende Nodes"
    milestones += "Interne Strukturen sind echte Komponenten"
    milestones += "Parent-Kontext ist im Baum verankert"

    vbox { v ?=>
      v.host.setClassNames(Seq("jfx2-demo"))
      
      div { d ?=>
        d.host.setClassNames(Seq("jfx2-demo__panel"))
        
        div { eyebrow ?=>
          eyebrow.host.setClassNames(Seq("jfx2-demo__eyebrow"))
          text("jfx2 architecture showcase")
        }

        div { title ?=>
          title.host.setClassNames(Seq("jfx2-demo__title"))
          text("Consistency & Truth")
        }

        div { copy ?=>
          copy.host.setClassNames(Seq("jfx2-demo__copy"))
          text("Diese Demo zeigt den neuen JFX2-Kern: Ein einziger Aufbaupfad f\u00fcr SSR, Hydration und Interaktivit\u00e4t.")
        }

        div { card ?=>
          card.host.setClassNames(Seq("jfx2-demo__card"))

          div { label ?=>
             label.host.setClassNames(Seq("jfx2-demo__label"))
             text("Dein Name")
          }

          val nameInput = input("name") { i ?=>
            i.host.setClassNames(Seq("jfx2-demo__input"))
            i.placeholder = "Name eingeben..."
            i.stringValueProperty.set(name.get)
          }
          
          nameInput.addDisposable(name.observe(nameInput.stringValueProperty.set))
          nameInput.addDisposable(nameInput.stringValueProperty.observe(name.set))

          val result = div { r ?=>
            r.host.setClassNames(Seq("jfx2-demo__result"))
          }
          
          def updateGreeting() = {
             result.host.setText(s"Hallo ${name.get}. Klicks: ${clicks.get}")
          }
          
          result.addDisposable(name.observe(_ => updateGreeting()))
          result.addDisposable(clicks.observe(_ => updateGreeting()))
          updateGreeting()

          button("Klick mich!") { b ?=>
            b.host.setClassNames(Seq("jfx2-demo__button"))
            b.onClick { _ => clicks.set(clicks.get + 1) }
          }
        }

        condition(clicks.map(_ % 2 == 0)) {
          div { s ?=>
            s.host.setClassNames(Seq("jfx2-demo__slot-status"))
            text("Gerade Anzahl an Klicks")
          }
          ()
        } {
          div { s ?=>
            s.host.setClassNames(Seq("jfx2-demo__slot-status", "is-odd"))
            text("Ungerade Anzahl an Klicks")
          }
          ()
        }

        div { mLabel ?=>
          mLabel.host.setClassNames(Seq("jfx2-demo__label"))
          text("Architektur-Meilensteine")
        }

        div { mList ?=>
          mList.host.setClassNames(Seq("jfx2-demo__milestones"))
          forEach(milestones) { item =>
            div { m ?=>
              m.host.setClassNames(Seq("jfx2-demo__milestone"))
              text(item)
            }
          }
        }
      }
      
      div { note ?=>
         note.host.setClassNames(Seq("jfx2-demo__note"))
         text("Nutze window.renderSsr('/') in der Konsole, um den SSR-Output zu sehen.")
      }
    }
  }

  @JSExportTopLevel("renderSsr")
  def renderSsr(path: String): String = {
    Ssr.renderToString {
      demo()
    }
  }
}
