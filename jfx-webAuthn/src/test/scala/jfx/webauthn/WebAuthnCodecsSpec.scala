package jfx.webauthn

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

class WebAuthnCodecsSpec extends AnyFlatSpec with Matchers {

  "Base64Url" should "round-trip arbitrary bytes" in {
    val bytes = new Uint8Array(js.Array[Short](1, 2, 3, 250.toShort, 251.toShort, 252.toShort))
    val encoded = Base64Url.encode(bytes)
    val decoded = Base64Url.decodeToBytes(encoded)

    encoded shouldBe "AQID-vv8"
    decoded.toArray.toSeq shouldBe Seq(1, 2, 3, 250, 251, 252)
  }

  "WebAuthnCodecs" should "decode registration options from standard JSON payloads" in {
    val json = js.Dynamic.literal(
      rp = js.Dynamic.literal(
        id = "example.com",
        name = "Example"
      ),
      user = js.Dynamic.literal(
        id = "dXNlcjEyMw",
        name = "ada@example.com",
        displayName = "Ada"
      ),
      challenge = "Y2hhbGxlbmdlLTEyMw",
      pubKeyCredParams = js.Array(
        js.Dynamic.literal(
          `type` = "public-key",
          alg = -7
        )
      ),
      excludeCredentials = js.Array(
        js.Dynamic.literal(
          id = "Y3JlZC0x",
          `type` = "public-key",
          transports = js.Array("internal", "usb")
        )
      ),
      authenticatorSelection = js.Dynamic.literal(
        residentKey = "preferred",
        userVerification = "required"
      ),
      attestation = "none"
    )

    val options = WebAuthnCodecs.creationOptionsFromJson(json)

    Base64Url.encode(options.challenge) shouldBe "Y2hhbGxlbmdlLTEyMw"
    Base64Url.encode(options.user.id) shouldBe "dXNlcjEyMw"
    options.rp.name shouldBe "Example"
    options.pubKeyCredParams.head.alg shouldBe -7
    Base64Url.encode(options.excludeCredentials.get.head.id) shouldBe "Y3JlZC0x"
    options.excludeCredentials.get.head.transports.get.toSeq shouldBe Seq("internal", "usb")
    options.authenticatorSelection.get.residentKey.get shouldBe "preferred"
    options.authenticatorSelection.get.userVerification.get shouldBe "required"
  }

  it should "decode authentication options from standard JSON payloads" in {
    val json = js.Dynamic.literal(
      challenge = "YXV0aC1jaGFsbGVuZ2U",
      rpId = "example.com",
      userVerification = "preferred",
      allowCredentials = js.Array(
        js.Dynamic.literal(
          id = "Y3JlZC0y",
          `type` = "public-key"
        )
      )
    )

    val options = WebAuthnCodecs.requestOptionsFromJson(json)

    Base64Url.encode(options.challenge) shouldBe "YXV0aC1jaGFsbGVuZ2U"
    options.rpId.get shouldBe "example.com"
    options.userVerification.get shouldBe "preferred"
    Base64Url.encode(options.allowCredentials.get.head.id) shouldBe "Y3JlZC0y"
  }
}
