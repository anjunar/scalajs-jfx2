package jfx.core

import jfx.core.meta.PackageClassLoader

/**
 * Registry for domain models.
 * Used for reflection-based operations like JSON mapping.
 */
object DomainRegistry {
  private val loader = PackageClassLoader("app.domain")

  def init(): Unit = {
    // Register domain classes here
    // Example: loader.register(() => new MyModel(), classOf[MyModel])
  }
}
