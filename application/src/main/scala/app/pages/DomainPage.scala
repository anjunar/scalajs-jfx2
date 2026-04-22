package app.pages

import app.components.Showcase.*
import app.domain.{Address, DomainRegistry, Email, User}
import jfx.action.Button.button
import jfx.core.component.Component
import jfx.core.component.Component.*
import jfx.core.meta.PackageClassLoader
import jfx.core.state.Property
import app.DemoI18n
import jfx.i18n.*
import jfx.form.Form
import jfx.form.Form.{controls, form}
import jfx.form.Input.input
import jfx.form.InputContainer.inputContainer
import jfx.form.SubForm.subForm
import jfx.json.JsonMapper
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox
import reflect.{Annotation, ClassDescriptor, PropertyDescriptor}

import scala.scalajs.js.JSON
import scala.util.Try

object DomainPage {
  def render(): Unit = {
    DomainRegistry.init()

    val mapper = new JsonMapper()
    val user = sampleUser()
    val jsonText = Property("")
    val roundtripText = Property("Not yet run.")
    val validationText = Property("Not yet validated.")
    val modelHeadline = Property("")

    def refreshJson(): Unit = {
      jsonText.set(JSON.stringify(mapper.serialize(user), space = 2))
      modelHeadline.set(s"${user.name.get} · ${user.email.get} · ${user.address.get.city.get}")
    }

    def observeUserChanges()(using Component): Unit = {
      addDisposable(user.name.observeWithoutInitial(_ => refreshJson()))
      addDisposable(user.email.observeWithoutInitial(_ => refreshJson()))
      addDisposable(user.address.observeWithoutInitial(_ => refreshJson()))
      addDisposable(user.address.get.street.observeWithoutInitial(_ => refreshJson()))
      addDisposable(user.address.get.city.observeWithoutInitial(_ => refreshJson()))
      addDisposable(user.emails.observeWithoutInitial(_ => refreshJson()))
    }

    refreshJson()

    val descriptors = PackageClassLoader.domains.getAllRegistered.sortBy(_.simpleName)
    val propertyCount = descriptors.map(_.resolved.properties.length).sum
    val annotationCount = descriptors.flatMap(_.resolved.properties).map(_.annotations.length).sum

    showcasePage(i18n"Domain", i18n"Reflection, binding, and JSON as one system.") {
      vbox {
        style { gap = "34px" }

        sectionIntro(
          i18n"Domain layer",
          i18n"The model is not just data, but metadata with behavior.",
          i18n"JFX2 registers domain classes through reflection descriptors. From that come automatic form bindings, validators from annotations, and JSON mapping that unwraps properties and lists correctly."
        )

        metricStrip(
          descriptors.length.toString -> "registered classes",
          propertyCount.toString -> "reflected properties",
          annotationCount.toString -> "validator annotations"
        )

        componentShowcase(
          i18n"Live domain cockpit",
          i18n"A user model bound to form controls. Changes flow directly into properties and JSON."
        ) {
          form(user) {
            val userForm = summon[Form[User]]
            observeUserChanges()

            vbox {
              style { gap = "22px" }

              div {
                classes = "showcase-result"
                vbox {
                  style { gap = "8px" }
                  div {
                    style { fontSize = "13px"; fontWeight = "700"; color = "var(--aj-ink-muted)" }
                    text = DemoI18n.text(i18n"Current model")
                  }
                  div {
                    style { fontSize = "22px"; fontWeight = "800" }
                    text = modelHeadline
                  }
                }
              }

              hbox {
                style {
                  gap = "18px"
                  alignItems = "stretch"
                  flexWrap = "wrap"
                }

                vbox {
                  style { gap = "14px"; flex = "1 1 260px" }
                  div {
                    style { fontWeight = "800" }
                  text = DemoI18n.text(i18n"User")
                  }
                  inputContainer(DemoI18n.text(i18n"Name")) {
                    input("name") {}
                  }
                  inputContainer(DemoI18n.text(i18n"Email")) {
                    input("email") {}
                  }
                }

                subForm("address") {
                  vbox {
                    style {
                      gap = "14px"
                      flex = "1 1 260px"
                      padding = "16px"
                      border = "1px solid var(--aj-surface-muted)"
                      borderRadius = "14px"
                      background = "var(--aj-surface)"
                    }
                    div {
                      style { fontWeight = "800" }
                    text = DemoI18n.text(i18n"Address")
                    }
                    inputContainer(DemoI18n.text(i18n"Street")) {
                      input("street") {}
                    }
                    inputContainer(DemoI18n.text(i18n"City")) {
                      input("city") {}
                    }
                  }
                }
              }

              hbox {
                style { gap = "10px"; flexWrap = "wrap" }

                button(DemoI18n.text(i18n"Validate")) {
                  onClick { _ =>
                    val errors = controls(using userForm).iterator.flatMap(_.validate(forceVisible = true)).toSeq
                    validationText.set(
                      if (errors.isEmpty) "All clean: the annotations produced no validation errors."
                      else s"${errors.length} errors: ${errors.mkString(", ")}"
                    )
                  }
                }

                button(DemoI18n.text(i18n"Roundtrip JSON")) {
                  onClick { _ =>
                    val result = Try {
                      val json = mapper.serialize(user)
                      val copy = mapper.deserialize[User](json)
                      s"Roundtrip OK: ${copy.name.get} from ${copy.address.get.city.get}"
                    }.getOrElse("Roundtrip failed.")
                    roundtripText.set(result)
                    refreshJson()
                  }
                }

                button(DemoI18n.text(i18n"Broken data")) {
                  onClick { _ =>
                    user.name.set("Al")
                    user.email.set("kein-mail-format")
                    user.address.get.street.set("")
                    user.address.get.city.set("")
                    refreshJson()
                  }
                }

                button(DemoI18n.text(i18n"Reset example")) {
                  onClick { _ =>
                    fillUser(user)
                    validationText.set("Not yet validated.")
                    roundtripText.set("Not yet run.")
                    refreshJson()
                  }
                }
              }

              hbox {
                style {
                  gap = "14px"
                  flexWrap = "wrap"
                }

                statusCard("Validation", validationText)
                statusCard("Mapper", roundtripText)
              }
            }
          }
        }

        componentShowcase(
          i18n"JSON mapping",
          i18n"Properties and ListProperties appear as normal JSON values during serialization."
        ) {
          codePanel("json", jsonText)
        }

        componentShowcase(
          i18n"Reflection registry",
          i18n"The registered domain classes provide names, types, accessors, and annotations."
        ) {
          hbox {
            style {
              gap = "18px"
              alignItems = "stretch"
              flexWrap = "wrap"
            }
            descriptors.foreach(descriptorCard)
          }
        }

        insightGrid(
          (i18n"Descriptor", i18n"Structure stays explicit", i18n"Classes, properties, and types remain readable at runtime without string magic in the form."),
          (i18n"Annotation", i18n"Rules stay on the model", i18n"Validation arises from the annotations on the domain properties."),
          (i18n"Mapper", i18n"JSON stays simple", i18n"Property[String] becomes String, ListProperty[Email] becomes Array.")
        )

        apiSection(
          i18n"DSL usage",
          i18n"Domain binding is name-based: input(\"email\") binds to user.email."
        ) {
          codeBlock("scala", """val user = new User()
form(user) {
  inputContainer("Name") {
    input("name") {}
  }

  subForm("address") {
    inputContainer("Stadt") {
      input("city") {}
    }
  }
}

val mapper = new JsonMapper()
val json = mapper.serialize(user)
val copy = mapper.deserialize[User](json)""")
        }
      }
    }
  }

  private def sampleUser(): User = {
    val user = new User()
    fillUser(user)
    user
  }

  private def fillUser(user: User): Unit = {
    user.name.set("Max Mustermann")
    user.email.set("max@example.com")
    val address = Option(user.address.get).getOrElse {
      val next = new Address()
      user.address.set(next)
      next
    }
    address.street.set("Musterstraße 1")
    address.city.set("Musterstadt")
    user.emails.setAll(Seq(email("max@example.com"), email("team@example.com")))
  }

  private def email(value: String): Email = {
    val email = new Email()
    email.value.set(value)
    email
  }

  private def statusCard(title: String, content: Property[String]): Unit = {
    div {
      classes = "showcase-result"
      style { flex = "1 1 260px" }
      vbox {
        style { gap = "8px" }
        div {
          style { fontWeight = "800" }
          text = title
        }
        div {
          style { color = "var(--aj-ink-muted)" }
          text = content
        }
      }
    }
  }

  private def codePanel(language: String, content: Property[String]): Unit = {
    vbox {
      classes = "code-block"
      div {
        classes = "code-block__lang"
        text = language
      }
      div {
        classes = "code-block__content"
        text = content
      }
    }
  }

  private def descriptorCard(descriptor: ClassDescriptor): Unit = {
    vbox {
      classes = "showcase-result"
      style { gap = "12px"; flex = "1 1 280px" }

      div {
        style { fontSize = "18px"; fontWeight = "800" }
        text = descriptor.simpleName
      }

      div {
        style { color = "var(--aj-ink-muted)"; fontSize = "13px" }
        text = descriptor.typeName
      }

      vbox {
        style { gap = "8px" }
        descriptor.resolved.properties.foreach(propertyRow)
      }
    }
  }

  private def propertyRow(property: PropertyDescriptor): Unit = {
    vbox {
      style {
        gap = "6px"
        padding = "10px"
        border = "1px solid var(--aj-surface-muted)"
        borderRadius = "10px"
        background = "var(--aj-canvas)"
      }

      hbox {
        style { gap = "8px"; alignItems = "baseline"; flexWrap = "wrap" }
        div {
          style { fontWeight = "700" }
          text = property.name
        }
        div {
          style { color = "var(--aj-ink-muted)"; fontSize = "12px" }
          text = shortTypeName(property.propertyType.typeName)
        }
      }

      val annotations = property.annotations.toSeq.map(annotationLabel)
      div {
        style { color = "var(--aj-ink-muted)"; fontSize = "12px" }
          text =
          if (annotations.isEmpty) "no annotations"
          else annotations.mkString(" · ")
      }
    }
  }

  private def annotationLabel(annotation: Annotation): String = {
    val params = annotation.parameters.toSeq
      .filterNot { case (_, value) => value == "" || value == null }
      .map { case (name, value) => s"$name=$value" }
      .mkString(", ")

    val name = shortTypeName(annotation.annotationClassName)
    if (params.isEmpty) name else s"$name($params)"
  }

  private def shortTypeName(typeName: String): String =
    Option(typeName).getOrElse("")
      .replace("scala.Predef.", "")
      .replace("java.lang.", "")
      .replace("jfx.core.state.", "")
      .replace("jfx.form.validators.", "")
      .replace("app.domain.", "")
      .split('.')
      .lastOption
      .getOrElse(typeName)
}
