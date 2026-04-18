package jfx.core.render

import org.scalajs.dom

import scala.collection.mutable

trait RenderBackend {

  def createElement(tagName: String, parent: Option[HostElement]): HostElement

  def isServer: Boolean

  def isHydrating: Boolean =
    false

}

object RenderBackend {

  private val stack = mutable.ArrayBuffer.empty[RenderBackend]

  def current: RenderBackend =
    stack.lastOption.getOrElse(BrowserRenderBackend)

  def withBackend[A](backend: RenderBackend)(block: => A): A = {
    stack += backend
    try block
    finally stack.remove(stack.length - 1)
  }

}

object BrowserRenderBackend extends RenderBackend {

  override def createElement(tagName: String, parent: Option[HostElement]): HostElement =
    new DomHostElement(tagName, dom.document.createElement(tagName))

  override def isServer: Boolean =
    false

}

final class SsrRenderBackend extends RenderBackend {

  override def createElement(tagName: String, parent: Option[HostElement]): HostElement =
    new SsrHostElement(tagName)

  override def isServer: Boolean =
    true

}

final class HydrationRenderBackend private (rootCursor: HydrationCursor) extends RenderBackend {

  private val cursors = mutable.Map.empty[HostElement, HydrationCursor]

  override def createElement(tagName: String, parent: Option[HostElement]): HostElement = {
    val cursor =
      parent.flatMap(p => cursors.get(p).orElse {
        val c = HydrationCursor.children(p.domElementOption.get)
        cursors.update(p, c)
        Some(c)
      }).getOrElse(rootCursor)

    val element = cursor.claim(tagName)
    new DomHostElement(tagName, element)
  }

  override def isServer: Boolean =
    false

  override def isHydrating: Boolean =
    true

}

object HydrationRenderBackend {

  def into(container: dom.Element): HydrationRenderBackend =
    new HydrationRenderBackend(HydrationCursor.children(container))

  def root(root: dom.Element): HydrationRenderBackend =
    new HydrationRenderBackend(HydrationCursor.exact(root))

}
