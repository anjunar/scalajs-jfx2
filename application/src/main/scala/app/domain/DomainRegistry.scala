package app.domain

import jfx.core.meta.PackageClassLoader

object DomainRegistry {
  private val loader = PackageClassLoader("app.domain")

  def init(): Unit = {
    loader.register(() => new Address(), classOf[Address])
    loader.register(() => new Email(), classOf[Email])
    loader.register(() => new User(), classOf[User])
    loader.register(() => new BlogDraft(), classOf[BlogDraft])
  }
}
