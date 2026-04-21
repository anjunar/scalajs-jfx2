package app.pages

import app.components.Showcase.*
import jfx.control.Image.*
import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.domain.Media
import jfx.form.ImageCropper.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox

object ImageCropperPage {
  def render(): Unit = {
    showcasePage("Image Cropper", "Bilder hochladen, zuschneiden und als Media speichern.") {
      vbox {
        style { gap = "34px" }

        sectionIntro(
          "Upload & Zuschnitt",
          "Der Cropper ist ein eigenes Formular-Control.",
          "ImageCropper kapselt Dateiauswahl, Crop-Dialog, Thumbnail-Erzeugung und Media-Binding. Damit bleibt die Image-Komponente schlicht und der interaktive Upload bekommt seinen eigenen Spielplatz."
        )

        componentShowcase(
          "Profilbild zuschneiden",
          "Quadratischer Zuschnitt mit Live-Vorschau des erzeugten Thumbnails."
        ) {
          val mediaProperty = Property[Media](null)

          vbox {
            style { gap = "20px" }

            imageCropper("profile-image", standalone = true) {
              placeholder = "Profilbild auswählen"
              aspectRatio = Some(1.0)
              outputMaxWidth = Some(400)
              thumbnailMaxWidth = 160
              thumbnailMaxHeight = 160
              windowTitle = "Profilbild zuschneiden"
              style { height = "320px" }
              addDisposable(valueProperty.observe(mediaProperty.set))
            }

            div {
              classes = "showcase-result"
              hbox {
                style { gap = "18px"; alignItems = "center"; flexWrap = "wrap" }

                image {
                  src = mediaProperty.map(mediaToThumbnailSrc)
                  alt = "Zugeschnittenes Profilbild"
                  style {
                    width = "112px"
                    height = "112px"
                    border = "1px solid var(--aj-surface-muted)"
                    borderRadius = "18px"
                    objectFit = "cover"
                    background = "var(--aj-canvas)"
                  }
                }

                vbox {
                  style { gap = "6px"; flex = "1 1 220px" }
                  div {
                    style { fontWeight = "bold" }
                    text = "Ergebnis"
                  }
                  div {
                    style { color = "var(--aj-ink-muted)" }
                    text = mediaProperty.map { media =>
                      if (media == null) "Noch kein Bild ausgewählt."
                      else {
                        val thumb = media.thumbnail.get
                        val contentType = Option(thumb.contentType.get).filter(_.nonEmpty).getOrElse("image/png")
                        val dataSize = Option(thumb.data.get).map(_.length).getOrElse(0)
                        s"$contentType, Thumbnail-Daten: $dataSize Zeichen"
                      }
                    }
                  }
                }
              }
            }
          }
        }

        componentShowcase(
          "Breites Seitenverhältnis",
          "Der gleiche Cropper kann direkt für Header- oder Bannerbilder konfiguriert werden."
        ) {
          imageCropper("banner-image", standalone = true) {
            placeholder = "Bannerbild auswählen"
            aspectRatio = Some(16.0 / 9.0)
            previewMaxWidth = 640
            previewMaxHeight = 360
            outputMaxWidth = Some(960)
            outputMaxHeight = Some(540)
            thumbnailMaxWidth = 320
            thumbnailMaxHeight = 180
            windowTitle = "Banner zuschneiden"
            style { height = "260px" }
          }
        }

        componentShowcase(
          "Readonly Zustand",
          "Wie andere Controls folgt auch der Cropper der gemeinsamen editable-DSL."
        ) {
          imageCropper("locked-image", standalone = true) {
            placeholder = "Readonly: Upload und Zuschnitt sind deaktiviert"
            editable = false
            style { height = "180px" }
          }
        }

        apiSection(
          "DSL Syntax",
          "Der Cropper bleibt ein normales Control und kann im Formular gebunden werden."
        ) {
          codeBlock("scala", """imageCropper("profileImage", standalone = true) {
  placeholder = "Profilbild auswählen"
  aspectRatio = Some(1.0)
  outputMaxWidth = Some(400)
  thumbnailMaxWidth = 160
  thumbnailMaxHeight = 160
  windowTitle = "Profilbild zuschneiden"
}

imageCropper("readonly", standalone = true) {
  editable = false
}""")
        }
      }
    }
  }

  private def mediaToThumbnailSrc(media: Media): String = {
    if (media == null || media.thumbnail.get == null) return ""

    val thumbnail = media.thumbnail.get
    val data = Option(thumbnail.data.get).map(_.trim).getOrElse("")
    if (data.isEmpty) ""
    else if (data.startsWith("data:") || data.startsWith("http://") || data.startsWith("https://") || data.startsWith("blob:")) data
    else {
      val contentType = Option(thumbnail.contentType.get).map(_.trim).filter(_.nonEmpty).getOrElse("image/png")
      s"data:$contentType;base64,$data"
    }
  }
}
