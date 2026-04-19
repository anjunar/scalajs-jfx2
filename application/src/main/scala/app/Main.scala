package app

import jfx.action.Button.*
import jfx.core.component.Box.*
import jfx.core.state.{ListProperty, Property}
import jfx.dsl.DslRuntime
import jfx.form.Input.*
import jfx.hydration.Hydration
import jfx.layout.Div.div
import jfx.layout.VBox.vbox
import jfx.layout.HBox.hbox
import jfx.ssr.Ssr
import jfx.statement.Condition.{condition, thenDo, elseDo}
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

    vbox {
      classes = Seq("jfx2-demo")
      
      div {
        classes = Seq("jfx2-demo__panel")
        
        div {
          classes = Seq("jfx2-demo__eyebrow")
          text = "jfx2 architecture showcase"
        }

        div {
          classes = Seq("jfx2-demo__title")
          text = "Consistency & Truth"
        }

        div {
          classes = Seq("jfx2-demo__copy")
          text = "Diese Demo zeigt den neuen JFX2-Kern: Ein einziger Aufbaupfad f\u00fcr SSR, Hydration und Interaktivit\u00e4t."
        }

        div {
          classes = Seq("jfx2-demo__card")

          div {
             classes = Seq("jfx2-demo__label")
             text = "Dein Name"
          }

          input("name") {
            classes = Seq("jfx2-demo__input")
            placeholder = "Name eingeben..."
            stringValueProperty.set(name.get)
            
            addDisposable(name.observe(stringValueProperty.set))
            addDisposable(stringValueProperty.observe(name.set))
          }

          val result = div {
            classes = Seq("jfx2-demo__result")
          }
          
          def updateGreeting() = {
             result.host.setText(s"Hallo ${name.get}. Klicks: ${clicks.get}")
          }
          
          addDisposable(name.observe(_ => updateGreeting()))(using result)
          addDisposable(clicks.observe(_ => updateGreeting()))(using result)
          updateGreeting()

          button("Klick mich!") {
            classes = Seq("jfx2-demo__button")
            onClick { _ => clicks.set(clicks.get + 1) }
          }
        }

        condition(clicks.map(_ % 2 == 0)) {
          thenDo {
            div {
              classes = Seq("jfx2-demo__slot-status")
              text = "Gerade Anzahl an Klicks"
            }
          }
          elseDo {
            div {
              classes = Seq("jfx2-demo__slot-status", "is-odd")
              text = "Ungerade Anzahl an Klicks"
            }
          }
        }

        div {
          classes = Seq("jfx2-demo__label")
          text = "Architektur-Meilensteine"
        }

        div {
          classes = Seq("jfx2-demo__milestones")
          forEach(milestones) { item =>
            div {
              classes = Seq("jfx2-demo__milestone")
              text = item
            }
          }
        }
      }
      
      div {
         classes = Seq("jfx2-demo__note")
         text = "Nutze window.renderSsr('/') in der Konsole, um den SSR-Output zu sehen."
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
