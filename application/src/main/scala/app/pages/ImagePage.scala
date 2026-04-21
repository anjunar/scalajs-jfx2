package app.pages

import jfx.control.Image.*
import jfx.form.ImageCropper.*
import jfx.core.component.Component.*
import jfx.layout.Div.div
import jfx.layout.VBox.vbox
import jfx.layout.HBox.hbox
import jfx.core.state.Property
import jfx.domain.Media
import app.components.Showcase.*

object ImagePage {
  def render() = {
    showcasePage("Bilder & Grafiken", "Ein Bild sagt mehr als tausend Zeilen Code.") {
      vbox {
        style { gap = "34px" }

        sectionIntro(
          "Visuelle Präsenz",
          "Bilder verleihen deiner Anwendung Tiefe und Identität.",
          "Die Image-Komponente bindet native <img> Elemente reaktiv in die DSL ein. Sie unterstützt statische Quellen sowie reaktive Properties für dynamische Galerien oder Profilbilder."
        )

        componentShowcase(
          "Statisches Bild",
          "Einfache Einbindung eines Bildes mit Quelle und Alternativtext."
        ) {
          hbox {
            style { gap = "20px"; alignItems = "center" }
            image {
              src = "https://images.unsplash.com/photo-1518791841217-8f162f1e1131?ixlib=rb-1.2.1&auto=format&fit=crop&w=300&q=80"
              alt = "Eine süße Katze"
              style { borderRadius = "8px"; width = "200px"; height = "auto"; boxShadow = "0 4px 12px rgba(0,0,0,0.1)" }
            }
            div {
              style { flex = "1" }
              text = "Dieses Bild wird statisch geladen. Die Image-Komponente sorgt dafür, dass alle Attribute korrekt gesetzt werden."
            }
          }
        }

        componentShowcase(
          "Dynamische Bildquelle",
          "Die Quelle kann an ein Property gebunden werden, um Bilder zur Laufzeit auszutauschen."
        ) {
          val currentSrc = Property("https://images.unsplash.com/photo-1543852786-1cf6624b9987?ixlib=rb-1.2.1&auto=format&fit=crop&w=300&q=80")
          
          vbox {
            style { gap = "15px"; alignItems = "center" }
            
            image {
              src = currentSrc
              alt = "Dynamisches Bild"
              style { borderRadius = "50%"; width = "150px"; height = "150px"; objectFit = "cover"; border = "3px solid var(--aj-accent)" }
            }

            hbox {
              style { gap = "10px" }
              jfx.action.Button.button("Katze 1") {
                onClick { _ => currentSrc.set("https://images.unsplash.com/photo-1543852786-1cf6624b9987?ixlib=rb-1.2.1&auto=format&fit=crop&w=300&q=80") }
              }
              jfx.action.Button.button("Katze 2") {
                onClick { _ => currentSrc.set("https://images.unsplash.com/photo-1533733508147-bb1797d7adad?ixlib=rb-1.2.1&auto=format&fit=crop&w=300&q=80") }
              }
            }
          }
        }

        componentShowcase(
          "Interaktiver Image Cropper",
          "Der ImageCropper ermöglicht das Hochladen und Zuschneiden von Bildern direkt im Browser."
        ) {
          val mediaProp = Property[Media](null)
          
          vbox {
            style { gap = "20px" }

            imageCropper("profile-image", standalone = true) {
              style { height = "300px" }
              aspectRatio = Some(1.0) // Quadratisch
              outputMaxWidth = Some(400)
              addDisposable(valueProperty.observe(mediaProp.set))
            }

            div {
              classes = "showcase-result"
              vbox {
                style { gap = "10px"; alignItems = "center" }
                div { text = "Vorschau des Ergebnisses:"; style { fontWeight = "bold" } }
                image {
                  style { width = "100px"; height = "100px"; border = "1px solid #ddd"; borderRadius = "4px" }
                  src = mediaProp.map { m =>
                    if (m == null) "" 
                    else {
                      val data = m.thumbnail.get.data.get
                      if (data.startsWith("data:")) data
                      else s"data:${m.thumbnail.get.contentType.get};base64,$data"
                    }
                  }
                }
              }
            }
          }
        }

        apiSection(
          "DSL Syntax",
          "Attribut-Zuweisungen erfolgen intuitiv innerhalb des Blocks."
        ) {
          codeBlock("scala", """image {
  src = "path/to/image.jpg"
  alt = "Beschreibung"
  style { width = "100%" }
}

// Oder reaktiv:
val myProperty = Property("...")
image {
  src = myProperty
}""")
        }
      }
    }
  }
}
