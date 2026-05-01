package jfx.form

import jfx.core.component.Component
import jfx.core.component.Component.*
import jfx.core.state.{Disposable, ListProperty, Property, ReadOnlyProperty}
import jfx.domain.{Media, Thumbnail}
import jfx.dsl.DslRuntime
import jfx.control.Image.*
import jfx.layout.Viewport
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.action.Button.button
import org.scalajs.dom.{CanvasRenderingContext2D, Event, File, FileReader, HTMLCanvasElement, HTMLImageElement, HTMLInputElement, PointerEvent, document, window}

import scala.compiletime.uninitialized
import scala.math.{abs, max, min}
import scala.util.control.NonFatal

class ImageCropper(val $name: String, override val $standalone: Boolean = false) extends Component with Control[Media] {

  override def tagName: String = "div"

  override val $valueProperty: Property[Media] = Property(null)

  val $sourceProperty: Property[Media] = Property(null)
  val $fileProperty: Property[File] = Property(null)
  val $validatorsProperty: ListProperty[ImageCropper.Validator] = ListProperty()

  var $aspectRatio: Option[Double] = None
  var $previewMaxWidth: Int = 480
  var $previewMaxHeight: Int = 360

  var $outputType: String = "image/png"
  var $outputQuality: Double = 0.92
  var $outputMaxWidth: Option[Int] = None
  var $outputMaxHeight: Option[Int] = None

  var $thumbnailMaxWidth: Int = 160
  var $thumbnailMaxHeight: Int = 160
  var $windowTitle: String = "Crop image"

  private var fileInput: ImageCropperFileInput = null
  private val previewSrcProperty: Property[String] = Property("")
  private val previewPlaceholderVisibleProperty: Property[Boolean] = Property(true)

  def disabled: Boolean =
    !$editableProperty.get

  def disabled_=(value: Boolean): Unit =
    $editableProperty.set(!value)

  def addValidator(validator: ImageCropper.Validator): Unit =
    $validatorsProperty += validator

  def observeValue(listener: Media => Unit): Disposable =
    $valueProperty.observe(listener)

  def read(): Media =
    $valueProperty.get

  override def compose(): Unit = {
    given Component = this
    addClass("image-cropper-field")
    addClass("image-cropper")
    tabIndex = 0

    hbox {
      addClass("toolbar")
      style { gap = "10px"; alignItems = "center" }

      ImageCropperFileInput { input =>
        fileInput = input
        addDisposable(input.host.addEventListener("change", _ => onFileChange()))
      }

      button() {
        text = $valueProperty.map(v => if (v != null) "Replace image" else "Choose image")
        visible = $editableProperty
        onClick { _ => if (fileInput != null) fileInput.click() }
      }

      button("Crop") {
        visible = $editableProperty.flatMap(e => $sourceProperty.map(s => e && s != null))
        onClick { _ => currentSource().foreach(openCropWindow) }
      }

      button("Clear") {
        visible = $editableProperty.flatMap(e => $valueProperty.map(v => e && v != null))
        onClick { _ => 
          setDirty(true)
          clear() 
        }
      }
    }

    div {
      style {
        flex = "1 1 auto"
        width = "100%"
        minWidth = "0"
        minHeight = "0"
        display = "flex"
        alignItems = "center"
        justifyContent = "center"
        position = "relative"
        overflow = "hidden"
        border = "1px solid var(--aj-surface-muted)"
        borderRadius = "6px"
        background = "var(--aj-canvas)"
      }

      image {
        addClass("preview")
        visible = previewSrcProperty.map(_.nonEmpty)
        src = previewSrcProperty
        alt = $placeholderProperty.map(placeholderText)
        style {
          width = "100%"
          height = "100%"
          minWidth = "0"
          minHeight = "0"
          border = "0"
          borderRadius = "0"
        }
      }

      div {
        visible = previewPlaceholderVisibleProperty
        style {
          width = "100%"
          height = "100%"
          display = "flex"
          alignItems = "center"
          justifyContent = "center"
          textAlign = "center"
          padding = "16px"
          color = "var(--aj-ink-muted)"
        }
        text = $placeholderProperty.map(p => if (p.trim.isEmpty) "No image selected" else p)
      }
    }

    addDisposable($valueProperty.observe { value =>
      $sourceProperty.set(value)
      syncPreview(value)
      validate()
    })

    addDisposable($validators.observe(_ => validate()))
    addDisposable($validatorsProperty.observe(_ => validate()))
    addDisposable($dirtyProperty.observe(_ => validate()))

    if (!$standalone) {
      try {
        val formContext = DslRuntime.service[FormContext]
        formContext.registerControl(this)
        addDisposable(() => formContext.unregisterControl(this))
      } catch {
        case _: Exception => 
      }
    }
  }

  private def onFileChange(): Unit = {
    if (fileInput == null) return

    val selectedFile = Option(fileInput.files).flatMap(files => Option(files.item(0))).orNull
    $fileProperty.set(selectedFile)

    if (selectedFile != null) {
      val reader = new FileReader()
      reader.onload = _ => {
        val dataUrl = Option(reader.result).map(_.toString).map(_.trim).filter(_.nonEmpty)
        dataUrl.foreach { encoded =>
          val media = mediaFromFile(selectedFile, encoded)
          setDirty(true)
          $sourceProperty.set(media)
          $valueProperty.set(media)
          openCropWindow(media)
        }
      }
      reader.readAsDataURL(selectedFile)
    }
  }

  private def currentSource(): Option[Media] =
    Option($sourceProperty.get).orElse(Option($valueProperty.get))
      .filter(media => Option(media.data.get).exists(_.trim.nonEmpty))

  private def openCropWindow(source: Media): Unit = {
    if (source == null || Option(source.data.get).forall(_.trim.isEmpty)) return

    val session = ImageCropperDialog.ImageCropperSession(
      initialValue = $valueProperty.get,
      initialDirty = $dirtyProperty.get
    )

    val conf = new Viewport.WindowConf(
      title = $windowTitle,
      component = () => ImageCropperDialog.build(this, source, session),
      onClose = Some { _ =>
        cancelCropSession(session)
      },
      resizable = true,
      draggable = true,
      rememberSize = true
    )

    session.windowConf = conf
    Viewport.addWindow(conf)
  }

  private[form] def applyCropSession(session: ImageCropperDialog.ImageCropperSession, media: Media): Unit = {
    if (media == null || session.closed) return

    session.applied = true
    session.closed = true
    setDirty(true)
    $valueProperty.set(media)
    closeCropWindow(session)
  }

  private[form] def cancelCropSession(session: ImageCropperDialog.ImageCropperSession): Unit = {
    if (session.closed) return

    session.closed = true
    if (!session.applied) {
      if (fileInput != null) fileInput.value = ""
      $fileProperty.set(null)
      $valueProperty.set(session.initialValue)
      $dirtyProperty.set(session.initialDirty)
    }
  }

  private[form] def closeCropWindow(session: ImageCropperDialog.ImageCropperSession): Unit = {
    if (session.windowConf != null) {
      Viewport.closeWindow(session.windowConf)
    }
  }

  private def mediaFromFile(file: File, dataUrl: String): Media = {
    val fileName = Option(file.name).getOrElse("")
    val contentType = Option(file.`type`).map(_.trim).filter(_.nonEmpty)
      .orElse(ImageCropper.mimeTypeFromDataUrl(dataUrl)).getOrElse($outputType)

    val base64 = ImageCropper.base64FromDataUrl(dataUrl).getOrElse(dataUrl)

    new Media(
      name = Property(fileName),
      contentType = Property(contentType),
      data = Property(base64),
      thumbnail = Property(new Thumbnail(
        name = Property(fileName),
        contentType = Property(contentType),
        data = Property("")
      ))
    )
  }

  private def syncPreview(media: Media): Unit = {
    val src = previewSrc(media).getOrElse("")
    previewSrcProperty.set(src)
    previewPlaceholderVisibleProperty.set(src.isEmpty)
  }

  private def placeholderText(value: String): String =
    Option(value).map(_.trim).filter(_.nonEmpty).getOrElse("No image selected")

  private def previewSrc(media: Media): Option[String] = {
    if (media == null) return None

    Option(media.thumbnail.get).flatMap { thumb =>
      val data = Option(thumb.data.get).map(_.trim).getOrElse("")
      if (data.isEmpty) None
      else {
        val ct = Option(thumb.contentType.get).map(_.trim).filter(_.nonEmpty)
          .orElse(Option(media.contentType.get).map(_.trim).filter(_.nonEmpty))
        ct.flatMap(ImageCropper.toDataUrl(_, data))
      }
    }.orElse {
      val data = Option(media.data.get).map(_.trim).getOrElse("")
      if (data.isEmpty) None
      else ImageCropper.toDataUrl(Option(media.contentType.get).getOrElse(""), data)
    }
  }

  override def validate(forceVisible: Boolean = false): Seq[String] = {
    val errors =
      if (!$editableProperty.get) Seq.empty
      else {
        val mediaErrors = $validators.iterator.flatMap(_.validate($valueProperty.get)).toSeq
        val currentData = Option($valueProperty.get).flatMap(media => Option(media.data.get)).getOrElse("")
        val imageErrors = $validatorsProperty.iterator.filterNot(_.validate(currentData)).map(_.message).toSeq
        mediaErrors ++ imageErrors
      }

    if (forceVisible || $dirtyProperty.get) {
      if (forceVisible) setDirty(true)
      $errorsProperty.setAll(errors)
    } else {
      $errorsProperty.setAll(Nil)
    }

    errors
  }

  private def clear(): Unit = {
    if (fileInput != null) fileInput.value = ""
    $fileProperty.set(null)
    $sourceProperty.set(null)
    $valueProperty.set(null)
  }
}

private final class ImageCropperFileInput(onReady: ImageCropperFileInput => Unit) extends Component {
  override def tagName: String = "input"

  override def compose(): Unit = {
    given Component = this
    attribute("type", "file")
    attribute("accept", "image/*")
    style { display = "none" }
    onReady(this)
  }

  def click(): Unit =
    inputElement.foreach(_.click())

  def files: org.scalajs.dom.FileList | Null =
    inputElement.map(_.files).orNull

  def value: String =
    inputElement.map(_.value).getOrElse("")

  def value_=(nextValue: String): Unit =
    inputElement.foreach(_.value = nextValue)

  private def inputElement: Option[HTMLInputElement] =
    host.domNode.collect { case input: HTMLInputElement => input }
}

private object ImageCropperFileInput {
  def apply(onReady: ImageCropperFileInput => Unit): ImageCropperFileInput =
    DslRuntime.build(new ImageCropperFileInput(onReady)) {}
}

private final class ImageCropperCanvas(onReady: ImageCropperCanvas => Unit) extends Component {
  override def tagName: String = "canvas"

  override def compose(): Unit = {
    given Component = this
    addClass("canvas")
    attribute("width", "1")
    attribute("height", "1")
    onReady(this)
  }

  def element: Option[HTMLCanvasElement] =
    host.domNode.collect { case canvas: HTMLCanvasElement => canvas }
}

private object ImageCropperCanvas {
  def apply(onReady: ImageCropperCanvas => Unit): ImageCropperCanvas =
    DslRuntime.build(new ImageCropperCanvas(onReady)) {}
}

private final class ImageCropperDialog(
  field: ImageCropper,
  source: Media,
  session: ImageCropperDialog.ImageCropperSession
) extends Component {

  override def tagName: String = "div"

  private var initialized = false
  private var previewScale = 1.0
  private var loadedImage: HTMLImageElement = null
  private var crop: ImageCropperDialog.CropRect = null
  private var drag: ImageCropperDialog.DragState = null
  private var livePending = false
  private var activePointerId: Double | Null = null

  private lazy val outCanvas = document.createElement("canvas").asInstanceOf[HTMLCanvasElement]
  private var mainCanvas: HTMLCanvasElement = null

  override def compose(): Unit = {
    given Component = this
    addClass("image-cropper")
    addClass("image-cropper-dialog")

    hbox {
      addClass("toolbar")
      style { gap = "10px"; padding = "10px" }
      
      button("Apply") {
        onClick { _ =>
          field.applyCropSession(session, cropToMedia())
        }
      }

      button("Reset") {
        onClick { _ =>
          if (loadedImage != null && mainCanvas != null) {
            crop = defaultCrop()
            render()
            scheduleLivePreview()
          }
        }
      }

      button("Close") {
        onClick { _ =>
          field.cancelCropSession(session)
          field.closeCropWindow(session)
        }
      }
    }

    div {
      addClass("canvas-wrap")
      style { padding = "10px"; display = "flex"; justifyContent = "center"; background = "var(--aj-canvas)" }

      ImageCropperCanvas { canvas =>
        mainCanvas = canvas.element.orNull
      }
    }
  }

  override def afterCompose(): Unit = {
    if (!initialized && mainCanvas != null) {
      initialized = true
      wireCanvasDragging()
      loadSourceImage()
    }
  }

  private def loadSourceImage(): Unit = {
    if (mainCanvas == null) return

    val image = document.createElement("img").asInstanceOf[HTMLImageElement]
    image.onload = _ => {
      loadedImage = image
      setupCanvasFor(image)
      crop = defaultCrop()
      render()
      scheduleLivePreview()
    }
    image.src = sourceToImgSrc(source)
  }

  private def sourceToImgSrc(media: Media): String = {
    val data = Option(media.data.get).map(_.trim).getOrElse("")
    if (data.isEmpty) ""
    else if (data.startsWith("data:") || data.startsWith("http://") || data.startsWith("https://") || data.startsWith("blob:")) data
    else {
      val contentType = Option(media.contentType.get).map(_.trim).filter(_.nonEmpty)
        .getOrElse(Option(field.$outputType).map(_.trim).filter(_.nonEmpty).getOrElse("image/png"))

      s"data:$contentType;base64,$data"
    }
  }

  private def setupCanvasFor(image: HTMLImageElement): Unit = {
    val width = max(1, image.naturalWidth)
    val height = max(1, image.naturalHeight)
    val scale = min(1.0, min(field.$previewMaxWidth.toDouble / width, field.$previewMaxHeight.toDouble / height))
    previewScale = scale
    mainCanvas.width = max(1, math.round(width * scale).toInt)
    mainCanvas.height = max(1, math.round(height * scale).toInt)
  }

  private def defaultCrop(): ImageCropperDialog.CropRect = {
    val cw = mainCanvas.width.toDouble
    val ch = mainCanvas.height.toDouble
    field.$aspectRatio match {
      case Some(ratio) if ratio > 0.0 =>
        var w = cw
        var h = w / ratio
        if (h > ch) { h = ch; w = h * ratio }
        ImageCropperDialog.CropRect((cw - w) / 2.0, (ch - h) / 2.0, w, h)
      case _ => ImageCropperDialog.CropRect(0, 0, cw, ch)
    }
  }

  private def render(): Unit = {
    val ctx = ImageCropper.context2d(mainCanvas)
    if (ctx == null || loadedImage == null || mainCanvas == null) return
    val cw = mainCanvas.width.toDouble
    val ch = mainCanvas.height.toDouble

    ctx.clearRect(0, 0, cw, ch)
    ctx.drawImage(loadedImage, 0, 0, cw, ch)

    Option(crop).map(_.normalize()).foreach { rect =>
      if (rect.w > 0 && rect.h > 0) {
        ctx.fillStyle = ImageCropper.themeColor("--aj-surface-backdrop", "rgba(0, 0, 0, 0.32)")
        ctx.fillRect(0, 0, cw, ch)

        ctx.save()
        ctx.beginPath()
        ctx.rect(rect.x, rect.y, rect.w, rect.h)
        ctx.clip()
        ctx.drawImage(loadedImage, 0, 0, cw, ch)
        ctx.restore()

        ctx.strokeStyle = ImageCropper.themeColor("--aj-ink-inverse", "rgba(255, 255, 255, 0.94)")
        ctx.lineWidth = 1
        ctx.strokeRect(rect.x + 0.5, rect.y + 0.5, max(0.0, rect.w - 1.0), max(0.0, rect.h - 1.0))

        val hs = 6.0
        def drawH(cx: Double, cy: Double): Unit = {
          ctx.fillStyle = ImageCropper.themeColor("--aj-ink-inverse", "rgba(255, 255, 255, 0.94)")
          ctx.fillRect(cx - hs/2, cy - hs/2, hs, hs)
          ctx.strokeStyle = ImageCropper.themeColor("--aj-surface-scrim", "rgba(0, 0, 0, 0.22)")
          ctx.strokeRect(cx - hs/2 + 0.5, cy - hs/2 + 0.5, hs - 1, hs - 1)
        }
        drawH(rect.x, rect.y)
        drawH(rect.x + rect.w, rect.y)
        drawH(rect.x, rect.y + rect.h)
        drawH(rect.x + rect.w, rect.y + rect.h)
      }
    }
  }

  private def scheduleLivePreview(): Unit =
    if (!session.closed && !livePending) {
      livePending = true
      window.requestAnimationFrame { _ =>
        livePending = false
        if (!session.closed) {
          val media = cropToMedia()
          if (media != null) field.$valueProperty.set(media)
        }
      }
    }

  private def wireCanvasDragging(): Unit = {
    if (mainCanvas == null) return

    val onPointerDown: Event => Unit = {
      case pointerEvent: PointerEvent if loadedImage != null && pointerEvent.button == 0 =>
        pointerEvent.preventDefault()
        pointerEvent.stopPropagation()
        activePointerId = pointerEvent.pointerId

        try {
          mainCanvas.setPointerCapture(pointerEvent.pointerId)
        } catch {
          case NonFatal(_) => ()
        }

        val point = canvasPoint(pointerEvent)
        val current = Option(crop).map(_.normalize()).orNull
        val mode = hitTest(current, point.x, point.y)
        drag = ImageCropperDialog.DragState(mode, point.x, point.y, current)
        if (mode == ImageCropperDialog.DragMode.New) {
          crop = ImageCropperDialog.CropRect(point.x, point.y, 1.0, 1.0)
        }
        render()

      case _ =>
        ()
    }

    val onPointerMove: Event => Unit = {
      case pointerEvent: PointerEvent if drag != null && loadedImage != null && activePointerId == pointerEvent.pointerId =>
        pointerEvent.preventDefault()
        pointerEvent.stopPropagation()

        val state = drag
        val point = canvasPoint(pointerEvent)
        val canvasWidth = mainCanvas.width.toDouble
        val canvasHeight = mainCanvas.height.toDouble
        val minSize = 8.0
        val ratio = field.$aspectRatio.filter(_ > 0.0)

        def clampMove(x: Double, y: Double, width: Double, height: Double): ImageCropperDialog.CropRect =
          ImageCropperDialog.CropRect(
            x.max(0.0).min(max(0.0, canvasWidth - width)),
            y.max(0.0).min(max(0.0, canvasHeight - height)),
            width,
            height
          )

        def clampRect(rect: ImageCropperDialog.CropRect): ImageCropperDialog.CropRect = {
          val normalized = rect.normalize()
          var x = normalized.x
          var y = normalized.y
          var width = max(minSize, normalized.w)
          var height = max(minSize, normalized.h)

          if (width > canvasWidth) width = canvasWidth
          if (height > canvasHeight) height = canvasHeight
          if (x < 0.0) x = 0.0
          if (y < 0.0) y = 0.0
          if (x + width > canvasWidth) x = canvasWidth - width
          if (y + height > canvasHeight) y = canvasHeight - height

          ImageCropperDialog.CropRect(x, y, width, height)
        }

        def applyAspect(anchorX: Double, anchorY: Double, dx: Double, dy: Double): ImageCropperDialog.CropRect =
          ratio match {
            case Some(value) =>
              val normalized = ImageCropperDialog.CropRect(anchorX, anchorY, dx, dy).normalize()
              val signX = if (dx >= 0.0) 1.0 else -1.0
              val signY = if (dy >= 0.0) 1.0 else -1.0
              val width0 = normalized.w
              val height0 = normalized.h

              val (width1, height1) =
                if (height0 == 0.0) {
                  (width0, width0 / value)
                } else if (width0 / height0 > value) {
                  (height0 * value, height0)
                } else {
                  (width0, width0 / value)
                }

              ImageCropperDialog.CropRect(anchorX, anchorY, width1 * signX, height1 * signY)

            case None =>
              ImageCropperDialog.CropRect(anchorX, anchorY, dx, dy)
          }

        crop =
          state.mode match {
            case ImageCropperDialog.DragMode.Move =>
              val startRect = state.startRect
              clampMove(
                startRect.x + (point.x - state.startX),
                startRect.y + (point.y - state.startY),
                startRect.w,
                startRect.h
              )

            case ImageCropperDialog.DragMode.New =>
              clampRect(applyAspect(state.startX, state.startY, point.x - state.startX, point.y - state.startY))

            case ImageCropperDialog.DragMode.ResizeNW =>
              val startRect = state.startRect
              val anchorX = startRect.x + startRect.w
              val anchorY = startRect.y + startRect.h
              clampRect(applyAspect(anchorX, anchorY, point.x - anchorX, point.y - anchorY))

            case ImageCropperDialog.DragMode.ResizeNE =>
              val startRect = state.startRect
              val anchorX = startRect.x
              val anchorY = startRect.y + startRect.h
              clampRect(applyAspect(anchorX, anchorY, point.x - anchorX, point.y - anchorY))

            case ImageCropperDialog.DragMode.ResizeSW =>
              val startRect = state.startRect
              val anchorX = startRect.x + startRect.w
              val anchorY = startRect.y
              clampRect(applyAspect(anchorX, anchorY, point.x - anchorX, point.y - anchorY))

            case ImageCropperDialog.DragMode.ResizeSE =>
              val startRect = state.startRect
              val anchorX = startRect.x
              val anchorY = startRect.y
              clampRect(applyAspect(anchorX, anchorY, point.x - anchorX, point.y - anchorY))
          }

        render()
        scheduleLivePreview()

      case _ =>
        ()
    }

    def finishPointerInteraction(event: PointerEvent): Unit = {
      event.preventDefault()
      event.stopPropagation()

      if (activePointerId == event.pointerId) {
        activePointerId = null
      }

      drag = null

      try {
        if (mainCanvas.hasPointerCapture(event.pointerId)) {
          mainCanvas.releasePointerCapture(event.pointerId)
        }
      } catch {
        case NonFatal(_) => ()
      }

      render()
    }

    val onPointerUp: Event => Unit = {
      case pointerEvent: PointerEvent if activePointerId == pointerEvent.pointerId =>
        finishPointerInteraction(pointerEvent)
      case _ =>
        ()
    }

    val onPointerCancel: Event => Unit = {
      case pointerEvent: PointerEvent if activePointerId == pointerEvent.pointerId =>
        finishPointerInteraction(pointerEvent)
      case _ =>
        ()
    }

    mainCanvas.addEventListener("pointerdown", onPointerDown)
    mainCanvas.addEventListener("lostpointercapture", onPointerCancel)
    window.addEventListener("pointermove", onPointerMove)
    window.addEventListener("pointerup", onPointerUp)
    window.addEventListener("pointercancel", onPointerCancel)

    addDisposable(() => mainCanvas.removeEventListener("pointerdown", onPointerDown))
    addDisposable(() => mainCanvas.removeEventListener("lostpointercapture", onPointerCancel))
    addDisposable(() => window.removeEventListener("pointermove", onPointerMove))
    addDisposable(() => window.removeEventListener("pointerup", onPointerUp))
    addDisposable(() => window.removeEventListener("pointercancel", onPointerCancel))
  }

  private def canvasPoint(e: PointerEvent): ImageCropperDialog.Point = {
    val r = mainCanvas.getBoundingClientRect()
    val scaleX = if (r.width == 0.0) 1.0 else mainCanvas.width.toDouble / r.width
    val scaleY = if (r.height == 0.0) 1.0 else mainCanvas.height.toDouble / r.height
    ImageCropperDialog.Point((e.clientX.toDouble - r.left) * scaleX, (e.clientY.toDouble - r.top) * scaleY)
  }

  private def hitTest(rect: ImageCropperDialog.CropRect, x: Double, y: Double): ImageCropperDialog.DragMode = {
    if (rect == null) return ImageCropperDialog.DragMode.New
    val r = rect.normalize(); val s = 10.0
    def near(px: Double, py: Double, cx: Double, cy: Double) = abs(px-cx) <= s && abs(py-cy) <= s
    if (near(x,y,r.x,r.y)) ImageCropperDialog.DragMode.ResizeNW
    else if (near(x,y,r.x+r.w,r.y)) ImageCropperDialog.DragMode.ResizeNE
    else if (near(x,y,r.x,r.y+r.h)) ImageCropperDialog.DragMode.ResizeSW
    else if (near(x,y,r.x+r.w,r.y+r.h)) ImageCropperDialog.DragMode.ResizeSE
    else if (x>=r.x && x<=r.x+r.w && y>=r.y && y<=r.y+r.h) ImageCropperDialog.DragMode.Move
    else ImageCropperDialog.DragMode.New
  }

  private def cropToMedia(): Media = {
    if (loadedImage == null || mainCanvas == null) return null
    val r = Option(crop).map(_.normalize()).getOrElse(defaultCrop().normalize())
    if (r.w <= 0.0 || r.h <= 0.0) return null

    val sx = r.x / previewScale
    val sy = r.y / previewScale
    val sw = r.w / previewScale
    val sh = r.h / previewScale
    var ow = max(1, math.round(sw).toInt)
    var oh = max(1, math.round(sh).toInt)

    if (field.$outputMaxWidth.exists(ow > _) || field.$outputMaxHeight.exists(oh > _)) {
      val scale = min(
        field.$outputMaxWidth.map(_.toDouble / ow.toDouble).getOrElse(1.0),
        field.$outputMaxHeight.map(_.toDouble / oh.toDouble).getOrElse(1.0)
      )
      ow = max(1, math.round(ow.toDouble * scale).toInt)
      oh = max(1, math.round(oh.toDouble * scale).toInt)
    }

    outCanvas.width = ow
    outCanvas.height = oh

    val ctx = ImageCropper.context2d(outCanvas)
    if (ctx == null) return null

    ctx.clearRect(0.0, 0.0, ow.toDouble, oh.toDouble)
    ctx.drawImage(loadedImage, sx, sy, sw, sh, 0.0, 0.0, ow.toDouble, oh.toDouble)

    val contentType = Option(field.$outputType).map(_.trim).filter(_.nonEmpty).getOrElse("image/png")
    val dataUrl = outCanvas.toDataURL(contentType, field.$outputQuality)
    val thumbData = ImageCropper.base64FromDataUrl(dataUrl).getOrElse(dataUrl)
    val sourceName = Option(source.name.get).getOrElse("")
    val sourceContentType = Option(source.contentType.get).map(_.trim).filter(_.nonEmpty).getOrElse(contentType)
    val sourceData = Option(source.data.get).map(value => ImageCropper.base64FromDataUrl(value).getOrElse(value)).getOrElse("")
    val thumbnailName = Option(source.thumbnail.get)
      .flatMap(thumb => Option(thumb.name.get).map(_.trim).filter(_.nonEmpty))
      .getOrElse(sourceName)

    new Media(
      name = Property(sourceName),
      contentType = Property(sourceContentType),
      data = Property(sourceData),
      thumbnail = Property(new Thumbnail(name = Property(thumbnailName), contentType = Property(contentType), data = Property(thumbData)))
    )
  }
}

object ImageCropperDialog {
  def build(field: ImageCropper, source: Media, session: ImageCropperSession): ImageCropperDialog =
    DslRuntime.build(new ImageCropperDialog(field, source, session)) {}

  final case class Point(x: Double, y: Double)
  final case class CropRect(x: Double, y: Double, w: Double, h: Double) {
    def normalize(): CropRect = CropRect(if (w >= 0) x else x + w, if (h >= 0) y else y + h, abs(w), abs(h))
  }
  final case class DragState(mode: DragMode, startX: Double, startY: Double, startRect: CropRect)
  enum DragMode { case New, Move, ResizeNW, ResizeNE, ResizeSW, ResizeSE }
  final case class ImageCropperSession(initialValue: Media, initialDirty: Boolean, var applied: Boolean = false, var closed: Boolean = false, var windowConf: Viewport.WindowConf = null)
}

object ImageCropper {
  trait Validator {
    def validate(value: String): Boolean
    def message: String
  }

  object Validator {
    def apply(errorMessage: String)(predicate: String => Boolean): Validator =
      new Validator {
        override def validate(value: String): Boolean =
          predicate(value)

        override def message: String =
          errorMessage
      }
  }

  def imageCropper(name: String, standalone: Boolean = false)(init: ImageCropper ?=> Unit): ImageCropper =
    DslRuntime.build(new ImageCropper(name, standalone))(init)

  def value(using c: ImageCropper): Media = c.$valueProperty.get
  def value_=(using c: ImageCropper)(media: Media): Unit = c.$valueProperty.set(media)

  def valueProperty(using c: ImageCropper): Property[Media] = c.$valueProperty

  def placeholder(using c: ImageCropper): String = c.$placeholder
  def placeholder_=(using c: ImageCropper)(value: String): Unit = c.$placeholder = value
  def placeholder_=(using c: ImageCropper)(value: ReadOnlyProperty[String]): Unit = c.$placeholder = value

  def editable(using c: ImageCropper): Boolean = c.$editable
  def editable_=(using c: ImageCropper)(value: Boolean): Unit = c.$editable = value
  def editableProperty(using c: ImageCropper): Property[Boolean] = c.$editableProperty

  def disabled(using c: ImageCropper): Boolean = c.disabled
  def disabled_=(using c: ImageCropper)(value: Boolean): Unit = c.disabled = value

  def aspectRatio(using c: ImageCropper): Option[Double] = c.$aspectRatio
  def aspectRatio_=(using c: ImageCropper)(value: Double): Unit = c.$aspectRatio = Some(value)
  def aspectRatio_=(using c: ImageCropper)(value: Option[Double]): Unit = c.$aspectRatio = value

  def previewMaxWidth(using c: ImageCropper): Int = c.$previewMaxWidth
  def previewMaxWidth_=(using c: ImageCropper)(value: Int): Unit = c.$previewMaxWidth = value

  def previewMaxHeight(using c: ImageCropper): Int = c.$previewMaxHeight
  def previewMaxHeight_=(using c: ImageCropper)(value: Int): Unit = c.$previewMaxHeight = value

  def outputType(using c: ImageCropper): String = c.$outputType
  def outputType_=(using c: ImageCropper)(value: String): Unit = c.$outputType = value

  def outputQuality(using c: ImageCropper): Double = c.$outputQuality
  def outputQuality_=(using c: ImageCropper)(value: Double): Unit = c.$outputQuality = value

  def outputMaxWidth(using c: ImageCropper): Option[Int] = c.$outputMaxWidth
  def outputMaxWidth_=(using c: ImageCropper)(value: Int): Unit = c.$outputMaxWidth = Some(value)
  def outputMaxWidth_=(using c: ImageCropper)(value: Option[Int]): Unit = c.$outputMaxWidth = value

  def outputMaxHeight(using c: ImageCropper): Option[Int] = c.$outputMaxHeight
  def outputMaxHeight_=(using c: ImageCropper)(value: Int): Unit = c.$outputMaxHeight = Some(value)
  def outputMaxHeight_=(using c: ImageCropper)(value: Option[Int]): Unit = c.$outputMaxHeight = value

  def thumbnailMaxWidth(using c: ImageCropper): Int = c.$thumbnailMaxWidth
  def thumbnailMaxWidth_=(using c: ImageCropper)(value: Int): Unit = c.$thumbnailMaxWidth = value

  def thumbnailMaxHeight(using c: ImageCropper): Int = c.$thumbnailMaxHeight
  def thumbnailMaxHeight_=(using c: ImageCropper)(value: Int): Unit = c.$thumbnailMaxHeight = value

  def windowTitle(using c: ImageCropper): String = c.$windowTitle
  def windowTitle_=(using c: ImageCropper)(value: String): Unit = c.$windowTitle = value
  def windowTitle_=(using c: ImageCropper)(value: ReadOnlyProperty[String]): Unit =
    c.addDisposable(value.observe(next => c.$windowTitle = Option(next).getOrElse("")))

  def addValidator(validator: Validator)(using c: ImageCropper): Unit =
    c.addValidator(validator)

  private[form] def mimeTypeFromDataUrl(dataUrl: String): Option[String] =
    if (!dataUrl.startsWith("data:")) None else {
      val semi = dataUrl.indexOf(';', 5); val comma = dataUrl.indexOf(',', 5)
      Seq(semi, comma).filter(_ > 5).sorted.headOption.map(dataUrl.substring(5, _))
    }

  private[form] def base64FromDataUrl(dataUrl: String): Option[String] =
    if (!dataUrl.startsWith("data:")) None else {
      val comma = dataUrl.indexOf(',', 5)
      if (comma < 0) None else Some(dataUrl.substring(comma + 1))
    }

  private[form] def toDataUrl(contentType: String, dataOrUrl: String): Option[String] = {
    val d = Option(dataOrUrl).map(_.trim).getOrElse("")
    if (d.isEmpty) None
    else if (d.startsWith("data:") || d.startsWith("http://") || d.startsWith("https://") || d.startsWith("blob:")) Some(d)
    else {
      val normalizedContentType = Option(contentType).map(_.trim).getOrElse("")
      if (normalizedContentType.isEmpty) None
      else Some(s"data:$normalizedContentType;base64,$d")
    }
  }

  private[form] def context2d(canvas: HTMLCanvasElement): CanvasRenderingContext2D =
    if (canvas == null) null
    else Option(canvas.getContext("2d")).map(_.asInstanceOf[CanvasRenderingContext2D]).orNull

  private[form] def themeColor(name: String, fallback: String): String = {
    val resolved =
      try {
        Option(window.getComputedStyle(document.documentElement).getPropertyValue(name))
          .map(_.trim)
          .filter(_.nonEmpty)
      } catch {
        case NonFatal(_) => None
      }

    resolved.getOrElse(fallback)
  }
}
