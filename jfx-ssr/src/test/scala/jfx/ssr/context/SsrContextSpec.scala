package jfx.ssr.context

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.scalajs.js

class SsrContextSpec extends AnyFlatSpec with Matchers {

  "SsrContext" should "expose request values before the render backend is active" in {
    val context = SsrRequestContext(
      origin = "http://localhost:8080",
      path = "/documents",
      cookie = "sid=abc"
    )

    SsrContext.currentOrigin shouldBe None

    SsrContext.withContext(context) {
      SsrContext.currentOrigin shouldBe Some("http://localhost:8080")
      SsrContext.currentPath shouldBe Some("/documents")
      SsrContext.currentCookie shouldBe Some("sid=abc")
    }

    SsrContext.currentOrigin shouldBe None
    SsrContext.currentPath shouldBe None
    SsrContext.currentCookie shouldBe None
  }

  it should "keep request values available while an async render promise is in flight" in {
    val context = SsrRequestContext(
      origin = "http://localhost:8080",
      path = "/document/documents/document/root",
      cookie = "sid=abc"
    )

    var seenOriginBeforeResolve: Option[String] = None
    var resolvePromise: js.Function1[js.Any, Unit] | Null = null

    val promise = SsrContext.withAsyncContext(context) {
      new js.Promise[String]((resolve, _) => {
        seenOriginBeforeResolve = SsrContext.currentOrigin
        resolvePromise = value => resolve(value.asInstanceOf[String])
      })
    }

    seenOriginBeforeResolve shouldBe Some("http://localhost:8080")
    SsrContext.currentOrigin shouldBe Some("http://localhost:8080")

    resolvePromise.nn("ok")
    promise
  }
}
