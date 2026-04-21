package jfx.domain

import jfx.core.state.Property
import java.util.UUID
import scala.annotation.meta.field

class Media(
    val id: Property[UUID] = Property(UUID.randomUUID()),
    var thumbnail: Property[Thumbnail | Null] = Property(null),
    var name: Property[String] = Property(""),
    var contentType: Property[String] = Property(""),
    var data: Property[String] = Property("")
)
