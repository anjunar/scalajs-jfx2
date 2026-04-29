package jfx.json

import scala.annotation.StaticAnnotation

class JsonIgnore(serializable: Boolean = false, deserializable : Boolean = false) extends StaticAnnotation