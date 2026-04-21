package app.pages

import app.domain.{Address, User}
import jfx.core.component.Component.*
import jfx.json.JsonMapper
import jfx.layout.Div.div
import jfx.layout.VBox.vbox
import jfx.action.Button.button
import scala.scalajs.js.JSON
import org.scalajs.dom

object DomainPage {
  def render() = {
    val mapper = new JsonMapper()
    val user = new User()
    user.name.set("Max Mustermann")
    user.email.set("max@example.com")
    user.address.get.street.set("Musterstraße 1")
    user.address.get.city.set("Musterstadt")

    vbox {
      style { padding = "20px"; gap = "10px" }

      div {
        style { fontSize = "24px"; fontWeight = "bold" }
        text = "Domain & JSON Mapping"
      }

      div {
        text = s"Original User: ${user.name.get} (${user.email.get}) from ${user.address.get.city.get}"
      }

      button("Serialize to JSON") {
        onClick { _ =>
          val json = mapper.serialize(user)
          dom.window.alert(s"Serialized JSON:\n${JSON.stringify(json, space = 2)}")
        }
      }

      button("Serialize and Deserialize") {
        onClick { _ =>
          val json = mapper.serialize(user)
          val deserialized = mapper.deserialize[User](json)
          dom.window.alert(s"Deserialized User Name: ${deserialized.name}")
        }
      }
    }
  }
}
