package jfx.core.component

import jfx.core.render.HostElement

import scala.collection.mutable

trait ComponentClasses extends ComponentCore {
  private val _baseClasses = mutable.ArrayBuffer.empty[String]
  private val _userClasses = mutable.ArrayBuffer.empty[String]

  def baseClasses: Seq[String] = _baseClasses.toSeq
  def userClasses: Seq[String] = _userClasses.toSeq

  def classes: Seq[String] = {
    _host match {
      case h: HostElement => h.attribute("class").getOrElse("").split(" ").toSeq.filter(_.nonEmpty)
      case _              => (_baseClasses ++ _userClasses).distinct.toSeq
    }
  }

  def classes_=(names: Seq[String]): Unit = setUserClasses(names)
  def classes_=(name: String): Unit = setUserClasses(name.split("\\s+").toSeq.filter(_.nonEmpty))

  private[jfx] def addBaseClass(name: String): Unit = {
    if (!_baseClasses.contains(name)) {
      _baseClasses += name
      syncClasses()
    }
  }

  private[jfx] def removeBaseClass(name: String): Unit = {
    if (_baseClasses.contains(name)) {
      _baseClasses -= name
      syncClasses()
    }
  }

  private[jfx] def setUserClasses(names: Seq[String]): Unit = {
    _userClasses.clear()
    _userClasses ++= names
    syncClasses()
  }

  private[jfx] override def syncClasses(): Unit = {
    _host match {
      case h: HostElement => h.setClassNames((_baseClasses ++ _userClasses).distinct.toSeq)
      case _              =>
    }
  }
}
