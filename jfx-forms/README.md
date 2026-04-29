# scalajs-jfx2-forms

Forms contains model-bound controls, validation and higher level form widgets.

## Install

```scala
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-core" % "2.1.3"
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-controls" % "2.1.3"
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-viewport" % "2.1.3"
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-forms" % "2.1.3"
```

## Form

```scala
import jfx.core.state.Property
import jfx.form.Form.form
import jfx.form.Input.input
import jfx.form.InputContainer.inputContainer
import jfx.form.validators.*

final class Account {
  @NotBlank("Required")
  val name: Property[String] = Property("")

  @EmailConstraint("Enter a valid email")
  val email: Property[String] = Property("")
}

val account = Account()

form(account) {
  inputContainer("Name") {
    input("name") {
      placeholder = "Ada"
    }
  }

  inputContainer("Email") {
    input("email") {
      placeholder = "ada@example.com"
    }
  }
}
```

## Input

```scala
import jfx.form.Input.input

input("name") {
  placeholder = "Full name"
  validators += jfx.form.validators.NotBlankValidator("Required")
}
```

Standalone inputs do not register with a parent form.

```scala
input("search", standalone = true) {
  placeholder = "Search"
}
```

## InputContainer

```scala
inputContainer("Display name") {
  input("name") {
    placeholder = "Visible profile name"
  }
}
```

## SubForm

```scala
import jfx.form.SubForm.subForm

final class Address {
  val street: Property[String] = Property("")
  val city: Property[String] = Property("")
}

final class User {
  val address: Property[Address] = Property(Address())
}

subForm[Address]("address") {
  inputContainer("Street") {
    input("street") {}
  }
  inputContainer("City") {
    input("city") {}
  }
}
```

## ArrayForm

```scala
import jfx.form.ArrayForm.*

final class Contact {
  val emails: jfx.core.state.ListProperty[String] = jfx.core.state.ListProperty()
}

arrayForm[String]("emails") {
  controlRenderer = index =>
    input(index.toString) {
      placeholder = s"Email ${index + 1}"
    }
}
```

## ComboBox

```scala
import jfx.form.ComboBox.comboBox

comboBox[String]("country") {
  items = Seq("Austria", "Germany", "Switzerland")
  placeholder = "Choose a country"
  converter = identity
  multiSelect = false
}
```

Custom renderers stay inside the DSL.

```scala
comboBox[String]("country") {
  items = Seq("Austria", "Germany")

  itemRenderer { (item, selected) =>
    jfx.layout.Div.div {
      text = selected.map(isSelected => if (isSelected) s"* $item" else item)
    }
  }

  footerRenderer {
    jfx.layout.Div.div {
      text = "End of list"
    }
  }
}
```

## ImageCropper

```scala
import jfx.domain.Media
import jfx.form.ImageCropper.imageCropper

imageCropper("avatar") {
  placeholder = "Choose image"
  aspectRatio = Some(1.0)
  previewMaxWidth = 240
  previewMaxHeight = 240
  outputType = "image/webp"
  outputQuality = 0.9
  value = Media()
}
```

## Validation Annotations

```scala
import jfx.form.validators.*

final class Profile {
  @NotBlank("Required")
  @Size(min = 2, max = 80, message = "Use 2 to 80 characters")
  val displayName: Property[String] = Property("")

  @Pattern(regex = "[a-z0-9-]+", message = "Use lowercase slugs")
  val slug: Property[String] = Property("")
}
```

## Server Error Responses

```scala
import jfx.form.ErrorResponse
import scala.scalajs.js

form(account) {
  setErrorResponses(Seq(
    ErrorResponse("Already registered", js.Array("email"))
  ))
}
```
