package jfx.device

import jfx.core.component.Component.*
import jfx.ssr.context.{SsrContext, SsrRequestContext}
import jfx.ssr.Ssr
import jfx.layout.Div.div
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DeviceSpec extends AnyFlatSpec with Matchers {

  "Device" should "resolve the mode from SSR cookies" in {
    val previous = Device.modeProperty.get

    try {
      val html = SsrContext.withContext(
        SsrRequestContext(cookie = "sid=abc; jfx-device=mobile")
      ) {
        Ssr.renderToString {
          Device.device {
            div {
              text = if (Device.isMobile) "mobile" else "desktop"
            }
          }
        }
      }

      html should include("mobile")
      Device.modeProperty.get shouldBe Device.Mode.Mobile
    } finally {
      Device.modeProperty.set(previous)
    }
  }

  it should "parse cookie values from the request header" in {
    val previous = Device.modeProperty.get

    try {
      SsrContext.withContext(SsrRequestContext(cookie = "foo=bar; jfx-device=desktop")) {
        Ssr.renderToString {
          Device.device {
            Device.mode shouldBe Device.Mode.Desktop
            Device.isDesktop shouldBe true
            Device.isMobile shouldBe false
          }
        }
      }
    } finally {
      Device.modeProperty.set(previous)
    }
  }
}
