package jfx.json

import jfx.core.meta.PackageClassLoader
import jfx.core.state.Property
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import reflect.macros.ReflectMacros
import scala.annotation.meta.field
import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal

class JsonMapperSpec extends AnyFlatSpec with Matchers {

  "JsonMapper annotations" should "serialize and deserialize JsonProperty field names" in {
    val mapper = JsonMapper()
    val person = AnnotatedPerson()
    person.name.set("Ada")
    person.age.set(37)

    val json = mapper.serialize(person, JsonMapperSpec.annotatedPersonMeta)

    json.selectDynamic("fullName").asInstanceOf[String].shouldBe("Ada")
    js.isUndefined(json.selectDynamic("name")).shouldBe(true)
    json.selectDynamic("age").asInstanceOf[Double].toInt.shouldBe(37)

    val restored = mapper.deserialize[AnnotatedPerson](literal(fullName = "Grace", age = 41), JsonMapperSpec.annotatedPersonMeta)

    restored.name.get.shouldBe("Grace")
    restored.age.get.shouldBe(41)
  }

  it should "ignore JsonIgnore properties during serialization and deserialization" in {
    val mapper = JsonMapper()
    val user = IgnoredSecret()
    user.visible.set("public")
    user.secret.set("private")

    val json = mapper.serialize(user, JsonMapperSpec.ignoredSecretMeta)

    json.selectDynamic("visible").asInstanceOf[String].shouldBe("public")
    js.isUndefined(json.selectDynamic("secret")).shouldBe(true)

    val restored = mapper.deserialize[IgnoredSecret](literal(visible = "client", secret = "tampered"), JsonMapperSpec.ignoredSecretMeta)

    restored.visible.get.shouldBe("client")
    restored.secret.get.shouldBe("")
  }

  it should "allow JsonIgnore properties to be serialization-only" in {
    val mapper = JsonMapper()
    val model = DirectionalIgnore()
    model.readOnly.set("server-value")

    val json = mapper.serialize(model, JsonMapperSpec.directionalIgnoreMeta)

    json.selectDynamic("readOnly").asInstanceOf[String].shouldBe("server-value")

    val restored = mapper.deserialize[DirectionalIgnore](literal(readOnly = "client-value"), JsonMapperSpec.directionalIgnoreMeta)

    restored.readOnly.get.shouldBe("")
  }

  it should "allow JsonIgnore properties to be deserialization-only" in {
    val mapper = JsonMapper()
    val model = DirectionalIgnore()
    model.writeOnly.set("server-secret")

    val json = mapper.serialize(model, JsonMapperSpec.directionalIgnoreMeta)

    js.isUndefined(json.selectDynamic("writeOnly")).shouldBe(true)

    val restored = mapper.deserialize[DirectionalIgnore](literal(writeOnly = "client-secret"), JsonMapperSpec.directionalIgnoreMeta)

    restored.writeOnly.get.shouldBe("client-secret")
  }

  it should "use JsonType values for polymorphic serialization and deserialization" in {
    val mapper = JsonMapper()
    val circle = Circle()
    circle.radius.set(12)

    val json = mapper.serialize(circle, JsonMapperSpec.shapeMeta)

    json.selectDynamic("@type").asInstanceOf[String].shouldBe("circle")
    json.selectDynamic("radius").asInstanceOf[Double].toInt.shouldBe(12)

    val circleJson = js.Dictionary[js.Any]("@type" -> "circle", "radius" -> 9).asInstanceOf[js.Dynamic]
    val restored = mapper.deserialize[Shape](circleJson, JsonMapperSpec.shapeMeta)

    restored.shouldBe(a[Circle])
    restored.asInstanceOf[Circle].radius.get.shouldBe(9)
  }
}

object JsonMapperSpec {
  private val loader = PackageClassLoader("jfx.json")

  val annotatedPersonMeta = loader.register(() => AnnotatedPerson(), classOf[AnnotatedPerson])
  val ignoredSecretMeta = loader.register(() => IgnoredSecret(), classOf[IgnoredSecret])
  val directionalIgnoreMeta = loader.register(() => DirectionalIgnore(), classOf[DirectionalIgnore])
  val circleMeta = loader.register(() => Circle(), classOf[Circle])

  val shapeMeta = {
    val descriptor = ReflectMacros.reflectWithAccessors[Shape]
    descriptor.bindRuntimeClass(classOf[Shape])
    loader.registerByDescriptor(descriptor)
    descriptor
  }
}

final class AnnotatedPerson(
  @(JsonProperty @field)("fullName")
  var name: Property[String] = Property(""),
  var age: Property[Int] = Property(0)
)

final class IgnoredSecret(
  var visible: Property[String] = Property(""),
  @(JsonIgnore @field)()
  var secret: Property[String] = Property("")
)

final class DirectionalIgnore(
  @(JsonIgnore @field)(serializable = true)
  var readOnly: Property[String] = Property(""),
  @(JsonIgnore @field)(deserializable = true)
  var writeOnly: Property[String] = Property("")
)

sealed abstract class Shape

@JsonType("circle")
final class Circle(
  var radius: Property[Int] = Property(0)
) extends Shape
