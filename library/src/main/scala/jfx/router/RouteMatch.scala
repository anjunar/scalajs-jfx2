package jfx.router

import scala.scalajs.js

final case class RouteMatch(
  route: Route,
  fullPath: String,
  params: js.Map[String, String],
  id: String = RouteMatch.nextId()
) {

  def pathParams: js.Map[String, String] =
    params

}

object RouteMatch {
  private var counter = 0

  private def nextId(): String = {
    counter += 1
    s"route-match-$counter"
  }
}
