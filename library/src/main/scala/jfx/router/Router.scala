package jfx.router

import jfx.core.component.{ChildSlot, NativeComponent, NodeComponent}
import jfx.core.render.RenderBackend
import jfx.core.state.{CompositeDisposable, Disposable, Property}
import jfx.dsl.{ComponentContext, DslRuntime, Scope}
import jfx.ssr.Ssr
import org.scalajs.dom
import org.scalajs.dom.{Event, Node, window}

import scala.collection.mutable
import scala.scalajs.js

final class Router private[router] (
  val routes: js.Array[Route],
  private val owner: NativeComponent[? <: dom.Element],
  private val slot: ChildSlot,
  private val scope: Scope,
  initialUrl: String
) extends Disposable {

  private final case class ParsedLocation(
    pathname: String,
    search: String,
    queryParams: js.Map[String, String]
  )

  val stateProperty: Property[RouterState] =
    Property(resolve(initialUrl))

  val contentProperty: Property[NodeComponent[? <: Node] | Null] =
    Property[NodeComponent[? <: Node] | Null](null)

  val loadingProperty: Property[Boolean] =
    Property(false)

  val errorProperty: Property[Option[Throwable]] =
    Property(None)

  private val disposable = new CompositeDisposable()
  private var disposed = false
  private var popStateListener: js.Function1[Event, Unit] | Null = null

  if (!RenderBackend.current.isServer) {
    val listener: js.Function1[Event, Unit] = _ => syncWithLocation()
    window.addEventListener("popstate", listener)
    popStateListener = listener
    disposable.add(() => window.removeEventListener("popstate", listener))
  }

  render(stateProperty.get)

  def state: RouterState =
    stateProperty.get

  def currentMatchOption: Option[RouteMatch] =
    state.currentMatchOption

  def currentRouteOption: Option[Route] =
    state.currentRouteOption

  def isMatched: Boolean =
    state.isMatched

  def navigate(path: String): Unit =
    navigate(path, replace = false)

  def replace(path: String): Unit =
    navigate(path, replace = true)

  def navigate(path: String, replace: Boolean): Unit = {
    if (disposed) return

    val nextState = resolve(path)

    if (!RenderBackend.current.isServer) {
      val fullNextUrl = Router.toFullPath(nextState.url)
      val currentFullUrl = Router.toFullPath(currentBrowserUrl())

      if (currentFullUrl != fullNextUrl) {
        if (replace) window.history.replaceState(null, "", fullNextUrl)
        else window.history.pushState(null, "", fullNextUrl)
      }
    }

    if (!sameState(stateProperty.get, nextState)) {
      stateProperty.setAlways(nextState)
      render(nextState)
    }
  }

  def reload(): Unit =
    if (!disposed) {
      val nextUrl =
        if (RenderBackend.current.isServer) state.url
        else currentBrowserUrl()

      val nextState = resolve(nextUrl)
      stateProperty.setAlways(nextState)
      render(nextState)
    }

  def renderRoute(path: String): Unit =
    navigate(path, replace = true)

  override def dispose(): Unit =
    if (!disposed) {
      disposed = true
      disposable.dispose()
      contentProperty.set(null)
      slot.dispose()
    }

  private def syncWithLocation(): Unit = {
    val nextState = resolve(currentBrowserUrl())
    if (!sameState(stateProperty.get, nextState)) {
      stateProperty.setAlways(nextState)
      render(nextState)
    }
  }

  private def render(state: RouterState): Unit = {
    loadingProperty.set(false)
    errorProperty.set(None)
    contentProperty.set(null)

    state.currentMatchOption match {
      case Some(routeMatch) =>
        loadingProperty.set(true)
        try {
          val children = renderRouteChildren(routeMatch, state)
          val content = children.headOption.orNull.asInstanceOf[NodeComponent[? <: Node] | Null]

          contentProperty.set(content)
          slot.replace(children)
          loadingProperty.set(false)
        } catch {
          case error: Throwable =>
            loadingProperty.set(false)
            errorProperty.set(Some(error))
            slot.clear()

            if (RenderBackend.current.isServer) throw error
            else dom.console.error(error)
        }

      case None =>
        slot.clear()
    }
  }

  private def renderRouteChildren(
    routeMatch: RouteMatch,
    state: RouterState
  ): Vector[NodeComponent[? <: dom.Node]] = {
    val context = RouteContext(
      path = state.path,
      url = state.url,
      fullPath = routeMatch.fullPath,
      pathParams = cloneParams(routeMatch.pathParams),
      queryParams = cloneParams(state.queryParams),
      state = state,
      routeMatch = routeMatch
    )

    collectOne {
      routeMatch.route.factory(context, scope)
    }
  }

  private def collectOne(
    render: => NodeComponent[? <: dom.Node] | Null
  ): Vector[NodeComponent[? <: dom.Node]] = {
    val buffer = mutable.ArrayBuffer.empty[NodeComponent[? <: dom.Node]]

    val returned =
      DslRuntime.withScope(scope) {
        DslRuntime.withComponentContext(
          ComponentContext(
            parent = Some(owner),
            attachOverride = Some(child => buffer += child)
          )
        ) {
          render
        }
      }

    if (returned != null && !buffer.exists(_ eq returned)) {
      buffer += returned
    }

    buffer.toVector
  }

  private def currentBrowserUrl(): String = {
    val relativePath = Router.toRelativePath(window.location.pathname)
    s"$relativePath${window.location.search}"
  }

  private def resolve(path: String): RouterState =
    parseLocation(path) match {
      case ParsedLocation(pathname, search, queryParams) =>
        RouteMatcher.resolveRoutes(routes, pathname).copy(
          queryParams = queryParams,
          search = search
        )
    }

  private def parseLocation(raw: String): ParsedLocation = {
    val sanitized = Option(raw).getOrElse("").trim
    val withoutHash = sanitized.takeWhile(_ != '#')
    val queryIndex = withoutHash.indexOf('?')
    val rawPath =
      if (queryIndex >= 0) withoutHash.substring(0, queryIndex)
      else withoutHash
    val rawSearch =
      if (queryIndex >= 0) withoutHash.substring(queryIndex)
      else ""

    val pathname = RouteMatcher.normalize(if (rawPath.isEmpty) "/" else rawPath)
    val normalizedSearch =
      if (rawSearch.isEmpty || rawSearch == "?") ""
      else if (rawSearch.startsWith("?")) rawSearch
      else s"?$rawSearch"

    ParsedLocation(
      pathname = pathname,
      search = normalizedSearch,
      queryParams = parseQueryParams(normalizedSearch)
    )
  }

  private def parseQueryParams(search: String): js.Map[String, String] = {
    val params = js.Map.empty[String, String]
    val query =
      if (search.startsWith("?")) search.drop(1)
      else search

    if (query.nonEmpty) {
      query.split("&").iterator
        .filter(_.nonEmpty)
        .foreach { pair =>
          val separator = pair.indexOf('=')
          val rawKey =
            if (separator >= 0) pair.substring(0, separator)
            else pair
          val rawValue =
            if (separator >= 0) pair.substring(separator + 1)
            else ""

          params += decodeQueryPart(rawKey) -> decodeQueryPart(rawValue)
        }
    }

    params
  }

  private def decodeQueryPart(value: String): String =
    js.URIUtils.decodeURIComponent(value.replace("+", " "))

  private def sameState(left: RouterState, right: RouterState): Boolean =
    left.path == right.path &&
      left.search == right.search &&
      left.matches.length == right.matches.length &&
      left.matches.zip(right.matches).forall { case (leftMatch, rightMatch) =>
        leftMatch.route == rightMatch.route &&
          leftMatch.fullPath == rightMatch.fullPath &&
          sameParams(leftMatch.params, rightMatch.params)
      } &&
      sameParams(left.queryParams, right.queryParams)

  private def sameParams(left: js.Map[String, String], right: js.Map[String, String]): Boolean =
    left.size == right.size &&
      left.forall { case (key, value) =>
        right.get(key).contains(value)
      }

  private def cloneParams(source: js.Map[String, String]): js.Map[String, String] = {
    val copy = js.Map.empty[String, String]
    source.foreach { case (key, value) =>
      copy += key -> value
    }
    copy
  }

}

object Router {

  private val SchemePattern =
    "^[a-zA-Z][a-zA-Z0-9+.-]*:".r

  def apply(routes: js.Array[Route])(using Scope): Router =
    router(routes)

  def router(routes: js.Array[Route]): Router =
    router(routes)({})

  def router(routes: js.Array[Route])(init: Router ?=> Unit): Router =
    DslRuntime.currentScope { currentScope =>
      val owner = currentNativeParent("router")
      val component =
        new Router(routes, owner, owner.reserveChildSlot(), currentScope, initialUrl(currentScope))

      owner.addDisposable(component)

      given Scope = currentScope
      given Router = component
      init

      component
    }

  def routerContent(using router: Router): Property[NodeComponent[? <: Node] | Null] =
    router.contentProperty

  def routerLoading(using router: Router): Boolean =
    router.loadingProperty.get

  def routerError(using router: Router): Option[Throwable] =
    router.errorProperty.get

  def routerState(using router: Router): RouterState =
    router.state

  def navigate(path: String)(using router: Router): Unit =
    router.navigate(path)

  def replace(path: String)(using router: Router): Unit =
    router.replace(path)

  def reload(using router: Router): Unit =
    router.reload()

  def renderRoute(router: Router, path: String): Unit =
    router.renderRoute(path)

  lazy val basePath: String = {
    if (RenderBackend.current.isServer) ""
    else {
      val baseElements = dom.document.getElementsByTagName("base")
      if (baseElements.length > 0) {
        val href = baseElements.item(0).asInstanceOf[dom.HTMLBaseElement].href
        try {
          val url = new dom.URL(href)
          val path = url.pathname

          if (path == "/" || path.isEmpty) ""
          else if (path.endsWith("/")) path.dropRight(1)
          else path
        } catch {
          case _: Throwable => ""
        }
      } else {
        ""
      }
    }
  }

  def isInternalPath(path: String): Boolean = {
    val value = Option(path).getOrElse("").trim

    value.nonEmpty &&
      !value.startsWith("#") &&
      !value.startsWith("//") &&
      SchemePattern.findPrefixOf(value).isEmpty
  }

  def toFullPath(path: String): String =
    toFullPath(path, basePath)

  def toFullPath(path: String, basePath: String): String = {
    val rawPath = Option(path).getOrElse("")
    if (!isInternalPath(rawPath)) return rawPath

    val normalizedBase = normalizeBasePath(basePath)

    if (normalizedBase.isEmpty) normalizePathWithLeadingSlash(rawPath)
    else if (rawPath.startsWith(normalizedBase)) rawPath
    else normalizedBase + normalizePathWithLeadingSlash(rawPath)
  }

  def toRelativePath(path: String): String = {
    if (basePath.isEmpty) path
    else if (path.startsWith(basePath)) {
      val relative = path.substring(basePath.length)
      if (relative.isEmpty) "/" else if (relative.startsWith("/")) relative else "/" + relative
    } else {
      path
    }
  }

  private def initialUrl(scope: Scope): String =
    if (RenderBackend.current.isServer) {
      try scope.inject[Ssr.Request].path
      catch {
        case _: NoSuchElementException => "/"
      }
    } else {
      val relativePath = toRelativePath(window.location.pathname)
      s"$relativePath${window.location.search}"
    }

  private def currentNativeParent(statementName: String): NativeComponent[? <: dom.Element] =
    DslRuntime.currentComponentContext().parent match {
      case Some(native: NativeComponent[?]) =>
        native

      case Some(other) =>
        throw IllegalStateException(
          s"$statementName can only be used inside a NativeComponent, but current parent is ${other.getClass.getSimpleName}"
        )

      case None =>
        throw IllegalStateException(s"$statementName can only be used inside a NativeComponent")
    }

  private def normalizeBasePath(value: String): String = {
    val raw = Option(value).getOrElse("").trim
    if (raw.isEmpty || raw == "/") ""
    else {
      val withLeadingSlash =
        if (raw.startsWith("/")) raw
        else "/" + raw

      if (withLeadingSlash.endsWith("/")) withLeadingSlash.dropRight(1)
      else withLeadingSlash
    }
  }

  private def normalizePathWithLeadingSlash(path: String): String =
    if (path.startsWith("/")) path
    else "/" + path

}
