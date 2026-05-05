# scalajs-jfx2-webauthn

WebAuthn provides browser facades and high-level helpers for passkey registration and authentication in Scala.js.

## Install

```scala
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-core" % "2.2.1"
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-webauthn" % "2.2.1"
```

## What It Covers

- browser capability checks
- registration via `navigator.credentials.create`
- authentication via `navigator.credentials.get`
- Base64Url helpers for server payloads
- conversion from standard WebAuthn JSON option payloads into browser-ready objects
- JSON payloads you can send back to your backend for verification

## Registration

```scala
import jfx.webauthn.WebAuthn
import scala.concurrent.ExecutionContext.Implicits.global

val optionsFromServer: scala.scalajs.js.Dynamic = ???

WebAuthn.register(optionsFromServer).map { credential =>
  val payload = credential.toJsObject
  // POST payload to backend
  credential
}
```

## Authentication

```scala
import jfx.webauthn.WebAuthn
import scala.concurrent.ExecutionContext.Implicits.global

val requestFromServer: scala.scalajs.js.Dynamic = ???

WebAuthn.authenticate(requestFromServer).map { assertion =>
  val payload = assertion.toJsObject
  // POST payload to backend
  assertion
}
```

## Capability Checks

```scala
import jfx.webauthn.WebAuthn
import scala.concurrent.ExecutionContext.Implicits.global

if (WebAuthn.isSupported) {
  WebAuthn.isUserVerifyingPlatformAuthenticatorAvailable().map { available =>
    println(s"Platform authenticator available: $available")
    available
  }
}
```

## Notes

This module handles the browser side of WebAuthn. Attestation validation, challenge generation, RP policy, signature verification and session handling still belong on the backend.
