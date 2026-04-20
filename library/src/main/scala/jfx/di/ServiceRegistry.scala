package jfx.di

import scala.collection.mutable
import scala.reflect.ClassTag

trait ServiceRegistry {
  def get[T](using manifest: ClassTag[T]): T
  def register[T](service: T)(using manifest: ClassTag[T]): Unit
}

class HierarchicalRegistry(parent: Option[ServiceRegistry] = None) extends ServiceRegistry {
  private val services = mutable.Map.empty[String, Any]
  
  override def get[T](using manifest: ClassTag[T]): T = {
    services.get(manifest.runtimeClass.getName)
      .map(_.asInstanceOf[T])
      .getOrElse(parent.map(_.get[T]).getOrElse(throw new Exception(s"Service ${manifest.runtimeClass.getName} not found")))
  }

  override def register[T](service: T)(using manifest: ClassTag[T]): Unit = {
    services.put(manifest.runtimeClass.getName, service)
  }
}
