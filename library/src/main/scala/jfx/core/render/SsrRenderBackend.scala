package jfx.core.render

final class SsrRenderBackend extends RenderBackend {
  override def isServer: Boolean = true
  override def nextCursor(parent: Option[HostElement]): Cursor = new SsrCursor()
}
