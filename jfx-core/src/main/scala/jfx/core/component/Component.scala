package jfx.core.component

trait Component extends ComponentClasses with ComponentEvents

object Component extends ComponentDsl {
  export jfx.dsl.StyleDsl.*
}
