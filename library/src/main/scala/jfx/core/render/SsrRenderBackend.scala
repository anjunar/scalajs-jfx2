package jfx.core.render

final class SsrRenderBackend extends RenderBackend {
  override def isServer: Boolean = true
  override def nextCursor(parent: Option[HostElement]): Cursor = {
    new SsrCursor(parent.map(_.asInstanceOf[SsrHostElement]))
  }
  override def insertionCursor(parent: HostElement, index: Int): Cursor = {
    new SsrCursor(Some(parent.asInstanceOf[SsrHostElement]), Some(index))
  }
}
