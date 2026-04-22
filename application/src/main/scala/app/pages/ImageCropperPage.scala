package app.pages

import app.DemoI18n
import app.components.Showcase.*
import jfx.control.Image.*
import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.i18n.*
import jfx.domain.Media
import jfx.form.ImageCropper.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox

object ImageCropperPage {
  def render(): Unit = {
    showcasePage(i18n"Image Cropper", i18n"Upload images, crop them, and store them as Media.") {
      vbox {
        style { gap = "34px" }

        sectionIntro(
          i18n"Upload & crop",
          i18n"The cropper is its own form control.",
          i18n"ImageCropper bundles file selection, crop dialog, thumbnail creation, and Media binding. That keeps the Image component simple and gives the interactive upload its own playground."
        )

        componentShowcase(
          i18n"Crop profile image",
          i18n"Square cropping with a live preview of the generated thumbnail."
        ) {
          val mediaProperty = Property[Media](null)

          vbox {
            style { gap = "20px" }

            imageCropper("profile-image", standalone = true) {
              placeholder = DemoI18n.text(i18n"Choose profile image")
              aspectRatio = Some(1.0)
              outputMaxWidth = Some(400)
              thumbnailMaxWidth = 160
              thumbnailMaxHeight = 160
              windowTitle = DemoI18n.text(i18n"Crop profile image")
              style { height = "320px" }
              addDisposable(valueProperty.observe(mediaProperty.set))
            }

            div {
              classes = "showcase-result"
              hbox {
                style { gap = "18px"; alignItems = "center"; flexWrap = "wrap" }

                image {
                  src = mediaProperty.map(mediaToThumbnailSrc)
                  alt = DemoI18n.text(i18n"Cropped profile image")
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
                    text = DemoI18n.text(i18n"Result")
                  }
                  div {
                    style { color = "var(--aj-ink-muted)" }
                    text = mediaProperty.map { media =>
                      if (media == null) DemoI18n.resolveNow(i18n"No image selected yet.")
                      else {
                        val thumb = media.thumbnail.get
                        val contentType = Option(thumb.contentType.get).filter(_.nonEmpty).getOrElse("image/png")
                        val dataSize = Option(thumb.data.get).map(_.length).getOrElse(0)
                        s"$contentType, thumbnail data: $dataSize characters"
                      }
                    }
                  }
                }
              }
            }
          }
        }

        componentShowcase(
          i18n"Wide aspect ratio",
          i18n"The same cropper can be configured directly for header or banner images."
        ) {
          imageCropper("banner-image", standalone = true) {
            placeholder = DemoI18n.text(i18n"Choose banner image")
            aspectRatio = Some(16.0 / 9.0)
            previewMaxWidth = 640
            previewMaxHeight = 360
            outputMaxWidth = Some(960)
            outputMaxHeight = Some(540)
            thumbnailMaxWidth = 320
            thumbnailMaxHeight = 180
            windowTitle = DemoI18n.text(i18n"Crop banner image")
            style { height = "260px" }
          }
        }

        componentShowcase(
          i18n"Readonly state",
          i18n"Like the other controls, the cropper follows the shared editable DSL."
        ) {
          imageCropper("locked-image", standalone = true) {
            placeholder = DemoI18n.text(i18n"Readonly: upload and cropping are disabled")
            editable = false
            style { height = "180px" }
          }
        }

        apiSection(
          i18n"DSL syntax",
          i18n"The cropper remains a normal control and can be bound in a form."
        ) {
          codeBlock("scala", """imageCropper("profileImage", standalone = true) {
  placeholder = "Choose profile image"
  aspectRatio = Some(1.0)
  outputMaxWidth = Some(400)
  thumbnailMaxWidth = 160
  thumbnailMaxHeight = 160
  windowTitle = "Crop profile image"
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
