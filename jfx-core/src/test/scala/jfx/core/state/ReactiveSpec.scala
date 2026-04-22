package jfx.core.state

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ReactiveSpec extends AnyFlatSpec with Matchers {

  "Property" should "notify observers on change" in {
    val p = Property("A")
    var lastValue = ""
    p.observe(v => lastValue = v)
    
    lastValue shouldBe "A"
    p.set("B")
    lastValue shouldBe "B"
  }

  it should "support mapping" in {
    val p = Property(10)
    val mapped = p.map(_ * 2)
    
    mapped.get shouldBe 20
    p.set(5)
    mapped.get shouldBe 10
  }

  "ListProperty" should "notify on additions" in {
    val list = new ListProperty[String]()
    var addedItem = ""
    list.observeChanges {
      case ListProperty.Add(item, _) => addedItem = item
      case _ =>
    }
    
    list += "New Item"
    addedItem shouldBe "New Item"
  }

  it should "handle removals" in {
    import scala.scalajs.js
    val list = new ListProperty[String](js.Array("A", "B", "C"))
    list.remove(1)
    list.get.toSeq shouldBe Seq("A", "C")
  }
}
