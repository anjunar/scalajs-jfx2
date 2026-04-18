package jfx.core.state

import scala.collection.mutable

final class CompositeDisposable extends Disposable {

  private val items = mutable.ArrayBuffer.empty[Disposable]

  def add(item: Disposable): Unit =
    items += item

  override def dispose(): Unit = {
    val snapshot = items.toVector
    items.clear()
    snapshot.foreach(_.dispose())
  }

}
