package jfx.webauthn

import scala.scalajs.js
import scala.scalajs.js.annotation.*
import scala.scalajs.js.typedarray.ArrayBuffer

@js.native
trait PublicKeyCredentialRpEntity extends js.Object {
  val id: js.UndefOr[String] = js.native
  val name: String = js.native
  val icon: js.UndefOr[String] = js.native
}

object PublicKeyCredentialRpEntity {
  def apply(name: String, id: Option[String] = None, icon: Option[String] = None): PublicKeyCredentialRpEntity = {
    val obj = js.Dynamic.literal(name = name)
    id.foreach(obj.updateDynamic("id")(_))
    icon.foreach(obj.updateDynamic("icon")(_))
    obj.asInstanceOf[PublicKeyCredentialRpEntity]
  }
}

@js.native
trait PublicKeyCredentialUserEntity extends js.Object {
  val id: ArrayBuffer = js.native
  val name: String = js.native
  val displayName: String = js.native
  val icon: js.UndefOr[String] = js.native
}

object PublicKeyCredentialUserEntity {
  def apply(id: ArrayBuffer, name: String, displayName: String, icon: Option[String] = None): PublicKeyCredentialUserEntity = {
    val obj = js.Dynamic.literal(id = id, name = name, displayName = displayName)
    icon.foreach(obj.updateDynamic("icon")(_))
    obj.asInstanceOf[PublicKeyCredentialUserEntity]
  }
}

@js.native
trait PublicKeyCredentialParameters extends js.Object {
  @JSName("type")
  val credentialType: String = js.native
  val alg: Int = js.native
}

object PublicKeyCredentialParameters {
  def apply(alg: Int, credentialType: String = "public-key"): PublicKeyCredentialParameters = {
    val obj = js.Dynamic.literal(alg = alg)
    obj.updateDynamic("type")(credentialType)
    obj.asInstanceOf[PublicKeyCredentialParameters]
  }
}

@js.native
trait PublicKeyCredentialDescriptor extends js.Object {
  @JSName("type")
  val credentialType: String = js.native
  val id: ArrayBuffer = js.native
  val transports: js.UndefOr[js.Array[String]] = js.native
}

object PublicKeyCredentialDescriptor {
  def apply(id: ArrayBuffer, credentialType: String = "public-key", transports: Seq[String] = Seq.empty): PublicKeyCredentialDescriptor = {
    val obj = js.Dynamic.literal(id = id)
    obj.updateDynamic("type")(credentialType)
    if (transports.nonEmpty) {
      obj.updateDynamic("transports")(js.Array(transports*))
    }
    obj.asInstanceOf[PublicKeyCredentialDescriptor]
  }
}

@js.native
trait AuthenticatorSelectionCriteria extends js.Object {
  val authenticatorAttachment: js.UndefOr[String] = js.native
  val residentKey: js.UndefOr[String] = js.native
  val requireResidentKey: js.UndefOr[Boolean] = js.native
  val userVerification: js.UndefOr[String] = js.native
}

object AuthenticatorSelectionCriteria {
  def apply(
    authenticatorAttachment: Option[String] = None,
    residentKey: Option[String] = None,
    requireResidentKey: Option[Boolean] = None,
    userVerification: Option[String] = None
  ): AuthenticatorSelectionCriteria = {
    val obj = js.Dynamic.literal()
    authenticatorAttachment.foreach(obj.updateDynamic("authenticatorAttachment")(_))
    residentKey.foreach(obj.updateDynamic("residentKey")(_))
    requireResidentKey.foreach(obj.updateDynamic("requireResidentKey")(_))
    userVerification.foreach(obj.updateDynamic("userVerification")(_))
    obj.asInstanceOf[AuthenticatorSelectionCriteria]
  }
}

@js.native
trait PublicKeyCredentialCreationOptions extends js.Object {
  val rp: PublicKeyCredentialRpEntity = js.native
  val user: PublicKeyCredentialUserEntity = js.native
  val challenge: ArrayBuffer = js.native
  val pubKeyCredParams: js.Array[PublicKeyCredentialParameters] = js.native
  val timeout: js.UndefOr[Double] = js.native
  val attestation: js.UndefOr[String] = js.native
  val excludeCredentials: js.UndefOr[js.Array[PublicKeyCredentialDescriptor]] = js.native
  val authenticatorSelection: js.UndefOr[AuthenticatorSelectionCriteria] = js.native
  val hints: js.UndefOr[js.Array[String]] = js.native
  val extensions: js.UndefOr[js.Object] = js.native
}

object PublicKeyCredentialCreationOptions {
  def apply(
    rp: PublicKeyCredentialRpEntity,
    user: PublicKeyCredentialUserEntity,
    challenge: ArrayBuffer,
    pubKeyCredParams: Seq[PublicKeyCredentialParameters],
    timeout: Option[Double] = None,
    attestation: Option[String] = None,
    excludeCredentials: Seq[PublicKeyCredentialDescriptor] = Seq.empty,
    authenticatorSelection: Option[AuthenticatorSelectionCriteria] = None,
    hints: Seq[String] = Seq.empty,
    extensions: Option[js.Object] = None
  ): PublicKeyCredentialCreationOptions = {
    val obj = js.Dynamic.literal(
      rp = rp,
      user = user,
      challenge = challenge,
      pubKeyCredParams = js.Array(pubKeyCredParams*)
    )
    timeout.foreach(obj.updateDynamic("timeout")(_))
    attestation.foreach(obj.updateDynamic("attestation")(_))
    if (excludeCredentials.nonEmpty) obj.updateDynamic("excludeCredentials")(js.Array(excludeCredentials*))
    authenticatorSelection.foreach(obj.updateDynamic("authenticatorSelection")(_))
    if (hints.nonEmpty) obj.updateDynamic("hints")(js.Array(hints*))
    extensions.foreach(obj.updateDynamic("extensions")(_))
    obj.asInstanceOf[PublicKeyCredentialCreationOptions]
  }
}

@js.native
trait PublicKeyCredentialRequestOptions extends js.Object {
  val challenge: ArrayBuffer = js.native
  val timeout: js.UndefOr[Double] = js.native
  val rpId: js.UndefOr[String] = js.native
  val allowCredentials: js.UndefOr[js.Array[PublicKeyCredentialDescriptor]] = js.native
  val userVerification: js.UndefOr[String] = js.native
  val hints: js.UndefOr[js.Array[String]] = js.native
  val extensions: js.UndefOr[js.Object] = js.native
}

object PublicKeyCredentialRequestOptions {
  def apply(
    challenge: ArrayBuffer,
    timeout: Option[Double] = None,
    rpId: Option[String] = None,
    allowCredentials: Seq[PublicKeyCredentialDescriptor] = Seq.empty,
    userVerification: Option[String] = None,
    hints: Seq[String] = Seq.empty,
    extensions: Option[js.Object] = None
  ): PublicKeyCredentialRequestOptions = {
    val obj = js.Dynamic.literal(challenge = challenge)
    timeout.foreach(obj.updateDynamic("timeout")(_))
    rpId.foreach(obj.updateDynamic("rpId")(_))
    if (allowCredentials.nonEmpty) obj.updateDynamic("allowCredentials")(js.Array(allowCredentials*))
    userVerification.foreach(obj.updateDynamic("userVerification")(_))
    if (hints.nonEmpty) obj.updateDynamic("hints")(js.Array(hints*))
    extensions.foreach(obj.updateDynamic("extensions")(_))
    obj.asInstanceOf[PublicKeyCredentialRequestOptions]
  }
}

@js.native
trait CredentialCreationOptions extends js.Object {
  val publicKey: PublicKeyCredentialCreationOptions = js.native
  val signal: js.UndefOr[js.Any] = js.native
}

object CredentialCreationOptions {
  def apply(publicKey: PublicKeyCredentialCreationOptions, signal: Option[js.Any] = None): CredentialCreationOptions = {
    val obj = js.Dynamic.literal(publicKey = publicKey)
    signal.foreach(obj.updateDynamic("signal")(_))
    obj.asInstanceOf[CredentialCreationOptions]
  }
}

@js.native
trait CredentialRequestOptions extends js.Object {
  val publicKey: PublicKeyCredentialRequestOptions = js.native
  val mediation: js.UndefOr[String] = js.native
  val signal: js.UndefOr[js.Any] = js.native
}

object CredentialRequestOptions {
  def apply(
    publicKey: PublicKeyCredentialRequestOptions,
    mediation: Option[String] = None,
    signal: Option[js.Any] = None
  ): CredentialRequestOptions = {
    val obj = js.Dynamic.literal(publicKey = publicKey)
    mediation.foreach(obj.updateDynamic("mediation")(_))
    signal.foreach(obj.updateDynamic("signal")(_))
    obj.asInstanceOf[CredentialRequestOptions]
  }
}

@js.native
trait AuthenticatorAttestationResponse extends js.Object {
  val clientDataJSON: ArrayBuffer = js.native
  val attestationObject: ArrayBuffer = js.native
  val getAuthenticatorData: js.UndefOr[js.Function0[ArrayBuffer]] = js.native
  val getPublicKey: js.UndefOr[js.Function0[ArrayBuffer | Null]] = js.native
  val getPublicKeyAlgorithm: js.UndefOr[js.Function0[Int]] = js.native
  val getTransports: js.UndefOr[js.Function0[js.Array[String]]] = js.native
}

@js.native
trait AuthenticatorAssertionResponse extends js.Object {
  val clientDataJSON: ArrayBuffer = js.native
  val authenticatorData: ArrayBuffer = js.native
  val signature: ArrayBuffer = js.native
  val userHandle: ArrayBuffer | Null = js.native
}

@js.native
trait PublicKeyCredential extends js.Object {
  val id: String = js.native
  val rawId: ArrayBuffer = js.native
  val response: js.Any = js.native
  def getClientExtensionResults(): js.Object = js.native
  val authenticatorAttachment: js.UndefOr[String] = js.native
  @JSName("type")
  val credentialType: String = js.native
}
