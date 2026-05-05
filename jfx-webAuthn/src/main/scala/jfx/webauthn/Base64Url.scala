package jfx.webauthn

import scala.scalajs.js
import scala.scalajs.js.typedarray.{ArrayBuffer, Uint8Array}

object Base64Url {

  def encode(buffer: ArrayBuffer): String =
    encode(new Uint8Array(buffer))

  def encode(bytes: Uint8Array): String = {
    val binary = uint8ArrayToBinary(bytes)
    val base64 = globalBase64Encode(binary)
    base64.replace("+", "-").replace("/", "_").replace("=", "")
  }

  def decode(value: String): ArrayBuffer = {
    val normalized = normalize(value)
    val binary = globalBase64Decode(normalized)
    val bytes = new Uint8Array(binary.length)
    var i = 0
    while (i < binary.length) {
      bytes(i) = binary.asInstanceOf[js.Dynamic].charCodeAt(i).asInstanceOf[Int].toShort
      i += 1
    }
    bytes.buffer
  }

  def decodeToBytes(value: String): Uint8Array =
    new Uint8Array(decode(value))

  private def normalize(value: String): String = {
    val padded = value.replace("-", "+").replace("_", "/")
    val remainder = padded.length % 4
    if (remainder == 0) padded else padded + ("=" * (4 - remainder))
  }

  private def uint8ArrayToBinary(bytes: Uint8Array): String = {
    val builder = new StringBuilder(bytes.length)
    var i = 0
    while (i < bytes.length) {
      builder.append(bytes(i).toChar)
      i += 1
    }
    builder.toString()
  }

  private def globalBase64Encode(value: String): String =
    js.Dynamic.global.btoa(value).asInstanceOf[String]

  private def globalBase64Decode(value: String): String =
    js.Dynamic.global.atob(value).asInstanceOf[String]
}
