package jfx.webauthn

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.typedarray.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

final case class RegistrationCredential(
  id: String,
  rawId: String,
  response: RegistrationResponse,
  authenticatorAttachment: Option[String] = None,
  clientExtensionResults: js.Object = js.Dynamic.literal(),
  credentialType: String = "public-key"
) {
  def toJsObject: js.Object = {
    val obj = js.Dynamic.literal(
      id = id,
      rawId = rawId,
      response = response.toJsObject,
      clientExtensionResults = clientExtensionResults
    )
    obj.updateDynamic("type")(credentialType)
    authenticatorAttachment.foreach(obj.updateDynamic("authenticatorAttachment")(_))
    obj.asInstanceOf[js.Object]
  }
}

final case class RegistrationResponse(
  clientDataJSON: String,
  attestationObject: String,
  transports: Seq[String] = Seq.empty,
  authenticatorData: Option[String] = None,
  publicKey: Option[String] = None,
  publicKeyAlgorithm: Option[Int] = None
) {
  def toJsObject: js.Object = {
    val obj = js.Dynamic.literal(
      clientDataJSON = clientDataJSON,
      attestationObject = attestationObject
    )
    if (transports.nonEmpty) obj.updateDynamic("transports")(js.Array(transports*))
    authenticatorData.foreach(obj.updateDynamic("authenticatorData")(_))
    publicKey.foreach(obj.updateDynamic("publicKey")(_))
    publicKeyAlgorithm.foreach(obj.updateDynamic("publicKeyAlgorithm")(_))
    obj.asInstanceOf[js.Object]
  }
}

final case class AuthenticationCredential(
  id: String,
  rawId: String,
  response: AuthenticationResponse,
  authenticatorAttachment: Option[String] = None,
  clientExtensionResults: js.Object = js.Dynamic.literal(),
  credentialType: String = "public-key"
) {
  def toJsObject: js.Object = {
    val obj = js.Dynamic.literal(
      id = id,
      rawId = rawId,
      response = response.toJsObject,
      clientExtensionResults = clientExtensionResults
    )
    obj.updateDynamic("type")(credentialType)
    authenticatorAttachment.foreach(obj.updateDynamic("authenticatorAttachment")(_))
    obj.asInstanceOf[js.Object]
  }
}

final case class AuthenticationResponse(
  clientDataJSON: String,
  authenticatorData: String,
  signature: String,
  userHandle: Option[String] = None
) {
  def toJsObject: js.Object = {
    val obj = js.Dynamic.literal(
      clientDataJSON = clientDataJSON,
      authenticatorData = authenticatorData,
      signature = signature
    )
    userHandle.foreach(obj.updateDynamic("userHandle")(_))
    obj.asInstanceOf[js.Object]
  }
}

object WebAuthn {

  given ExecutionContext = ExecutionContext.global

  def isSupported: Boolean = {
    !js.isUndefined(js.Dynamic.global.selectDynamic("navigator")) &&
    !js.isUndefined(js.Dynamic.global.selectDynamic("PublicKeyCredential")) &&
    !js.isUndefined(js.Dynamic.global.selectDynamic("navigator").selectDynamic("credentials"))
  }

  def isUserVerifyingPlatformAuthenticatorAvailable(): Future[Boolean] =
    isUserVerifyingPlatformAuthenticatorAvailablePromise().toFuture

  def isUserVerifyingPlatformAuthenticatorAvailablePromise(): js.Promise[Boolean] =
    if (!isSupported) {
      js.Promise.resolve(false)
    } else {
      js.Dynamic.global.PublicKeyCredential
        .isUserVerifyingPlatformAuthenticatorAvailable()
        .asInstanceOf[js.Promise[Boolean]]
    }

  def register(options: js.Dynamic): Future[RegistrationCredential] =
    registerPromise(options).toFuture

  def register(options: PublicKeyCredentialCreationOptions): Future[RegistrationCredential] =
    registerPromise(options).toFuture

  def authenticate(options: js.Dynamic): Future[AuthenticationCredential] =
    authenticatePromise(options).toFuture

  def authenticate(options: PublicKeyCredentialRequestOptions): Future[AuthenticationCredential] =
    authenticatePromise(options).toFuture

  def registerPromise(options: js.Dynamic): js.Promise[RegistrationCredential] =
    createCredential(WebAuthnCodecs.creationOptionsFromJson(options))

  def registerPromise(options: PublicKeyCredentialCreationOptions): js.Promise[RegistrationCredential] =
    createCredential(options)

  def authenticatePromise(options: js.Dynamic): js.Promise[AuthenticationCredential] =
    getCredential(WebAuthnCodecs.requestOptionsFromJson(options))

  def authenticatePromise(options: PublicKeyCredentialRequestOptions): js.Promise[AuthenticationCredential] =
    getCredential(options)

  private def createCredential(options: PublicKeyCredentialCreationOptions): js.Promise[RegistrationCredential] = {
    ensureSupported()
    val request = CredentialCreationOptions(options)
    mapPromise(
      js.Dynamic.global.navigator.credentials
        .create(request)
        .asInstanceOf[js.Promise[PublicKeyCredential]]
    )(serializeRegistration)
  }

  private def getCredential(options: PublicKeyCredentialRequestOptions): js.Promise[AuthenticationCredential] = {
    ensureSupported()
    val request = CredentialRequestOptions(options)
    mapPromise(
      js.Dynamic.global.navigator.credentials
        .get(request)
        .asInstanceOf[js.Promise[PublicKeyCredential]]
    )(serializeAuthentication)
  }

  private def serializeRegistration(credential: PublicKeyCredential): RegistrationCredential = {
    val response = credential.response.asInstanceOf[AuthenticatorAttestationResponse]
    RegistrationCredential(
      id = credential.id,
      rawId = Base64Url.encode(credential.rawId),
      response = RegistrationResponse(
        clientDataJSON = Base64Url.encode(response.clientDataJSON),
        attestationObject = Base64Url.encode(response.attestationObject),
        transports = response.getTransports.map(_().toSeq).getOrElse(Seq.empty),
        authenticatorData = response.getAuthenticatorData.toOption.map(fn => Base64Url.encode(fn())),
        publicKey = response.getPublicKey.toOption.flatMap(fn => Option(fn())).map(buffer => Base64Url.encode(buffer)),
        publicKeyAlgorithm = response.getPublicKeyAlgorithm.toOption.map(_())
      ),
      authenticatorAttachment = credential.authenticatorAttachment.toOption,
      clientExtensionResults = credential.getClientExtensionResults(),
      credentialType = credential.credentialType
    )
  }

  private def serializeAuthentication(credential: PublicKeyCredential): AuthenticationCredential = {
    val response = credential.response.asInstanceOf[AuthenticatorAssertionResponse]
    AuthenticationCredential(
      id = credential.id,
      rawId = Base64Url.encode(credential.rawId),
      response = AuthenticationResponse(
        clientDataJSON = Base64Url.encode(response.clientDataJSON),
        authenticatorData = Base64Url.encode(response.authenticatorData),
        signature = Base64Url.encode(response.signature),
        userHandle = Option(response.userHandle).map(Base64Url.encode)
      ),
      authenticatorAttachment = credential.authenticatorAttachment.toOption,
      clientExtensionResults = credential.getClientExtensionResults(),
      credentialType = credential.credentialType
    )
  }

  private def ensureSupported(): Unit = {
    if (!isSupported) {
      throw new IllegalStateException("WebAuthn is not supported in this environment")
    }
  }

  private def mapPromise[A, B](promise: js.Promise[A])(f: A => B): js.Promise[B] =
    promise.toFuture.map(f).toJSPromise
}

object WebAuthnCodecs {

  def creationOptionsFromJson(value: js.Dynamic): PublicKeyCredentialCreationOptions = {
    val rp = value.selectDynamic("rp").asInstanceOf[js.Dynamic]
    val user = value.selectDynamic("user").asInstanceOf[js.Dynamic]
    val selection =
      selectOption(value, "authenticatorSelection").map { raw =>
        AuthenticatorSelectionCriteria(
          authenticatorAttachment = optionString(raw.asInstanceOf[js.Dynamic], "authenticatorAttachment"),
          residentKey = optionString(raw.asInstanceOf[js.Dynamic], "residentKey"),
          requireResidentKey = optionBoolean(raw.asInstanceOf[js.Dynamic], "requireResidentKey"),
          userVerification = optionString(raw.asInstanceOf[js.Dynamic], "userVerification")
        )
      }

    PublicKeyCredentialCreationOptions(
      rp = PublicKeyCredentialRpEntity(
        id = optionString(rp, "id"),
        name = requiredString(rp, "name"),
        icon = optionString(rp, "icon")
      ),
      user = PublicKeyCredentialUserEntity(
        id = decodeBase64Field(user, "id"),
        name = requiredString(user, "name"),
        displayName = requiredString(user, "displayName"),
        icon = optionString(user, "icon")
      ),
      challenge = decodeBase64Field(value, "challenge"),
      pubKeyCredParams = arrayOf(value, "pubKeyCredParams")
        .map { item =>
          PublicKeyCredentialParameters(
            alg = requiredInt(item, "alg"),
            credentialType = optionString(item, "type").getOrElse("public-key")
          )
        }
        .toSeq,
      timeout = optionDouble(value, "timeout"),
      attestation = optionString(value, "attestation"),
      excludeCredentials = arrayOf(value, "excludeCredentials")
        .map(descriptorFromJson)
        .toSeq,
      authenticatorSelection = selection,
      hints = stringArray(value, "hints").toSeq,
      extensions = optionObject(value, "extensions")
    )
  }

  def requestOptionsFromJson(value: js.Dynamic): PublicKeyCredentialRequestOptions =
    PublicKeyCredentialRequestOptions(
      challenge = decodeBase64Field(value, "challenge"),
      timeout = optionDouble(value, "timeout"),
      rpId = optionString(value, "rpId"),
      allowCredentials = arrayOf(value, "allowCredentials").map(descriptorFromJson).toSeq,
      userVerification = optionString(value, "userVerification"),
      hints = stringArray(value, "hints").toSeq,
      extensions = optionObject(value, "extensions")
    )

  private def descriptorFromJson(value: js.Dynamic): PublicKeyCredentialDescriptor =
    PublicKeyCredentialDescriptor(
      id = decodeBase64Field(value, "id"),
      credentialType = optionString(value, "type").getOrElse("public-key"),
      transports = stringArray(value, "transports").toSeq
    )

  private def decodeBase64Field(value: js.Dynamic, field: String): ArrayBuffer =
    Base64Url.decode(requiredString(value, field))

  private def requiredString(value: js.Dynamic, field: String): String =
    select(value, field).toString

  private def requiredInt(value: js.Dynamic, field: String): Int =
    select(value, field).asInstanceOf[Double].toInt

  private def optionString(value: js.Dynamic, field: String): Option[String] =
    selectOption(value, field).map(_.toString)

  private def optionBoolean(value: js.Dynamic, field: String): Option[Boolean] =
    selectOption(value, field).map(_.asInstanceOf[Boolean])

  private def optionDouble(value: js.Dynamic, field: String): Option[Double] =
    selectOption(value, field).map(_.asInstanceOf[Double])

  private def optionObject(value: js.Dynamic, field: String): Option[js.Object] =
    selectOption(value, field).map(_.asInstanceOf[js.Object])

  private def arrayOf(value: js.Dynamic, field: String): js.Array[js.Dynamic] =
    selectOption(value, field)
      .map(_.asInstanceOf[js.Array[js.Dynamic]])
      .getOrElse(js.Array())

  private def stringArray(value: js.Dynamic, field: String): js.Array[String] =
    selectOption(value, field)
      .map(_.asInstanceOf[js.Array[String]])
      .getOrElse(js.Array())

  private def select(value: js.Dynamic, field: String): js.Any =
    selectOption(value, field).getOrElse {
      throw new IllegalArgumentException(s"Missing WebAuthn field: $field")
    }

  private def selectOption(value: js.Dynamic, field: String): Option[js.Any] = {
    val selected = value.selectDynamic(field)
    if (selected == null || js.isUndefined(selected)) None else Some(selected.asInstanceOf[js.Any])
  }
}
