package jfx.domain

import jfx.core.state.Property
import java.util.UUID
import scala.annotation.meta.field

class Thumbnail(
    val id: Property[UUID] = Property(UUID.randomUUID()),
    var name: Property[String] = Property(""),
    var contentType: Property[String] = Property(""),
    var data: Property[String] = Property("")
)
