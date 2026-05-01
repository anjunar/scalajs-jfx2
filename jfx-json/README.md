# scalajs-jfx2-json

Json maps reflection metadata to JavaScript JSON objects. It understands plain fields, `Property[T]`, `ListProperty[T]`, options, maps, collections, primitive values and UUIDs.

## Install

```scala
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-core" % "2.2.0"
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-json" % "2.2.0"
```

## Serialize

```scala
import jfx.core.state.Property
import jfx.json.JsonMapper

final class Account {
  val name: Property[String] = Property("")
  val email: Property[String] = Property("")
}

val account = Account()
account.name.set("Ada")
account.email.set("ada@example.com")

val mapper = new JsonMapper()
val json = mapper.serialize(account)
```

## Deserialize

```scala
val copy = mapper.deserialize[Account](json)

copy.name.get
copy.email.get
```

## Collections

```scala
import jfx.core.state.ListProperty

final class Team {
  val members: ListProperty[Account] = ListProperty()
}

val team = Team()
team.members += account

val teamJson = mapper.serialize(team)
val restored = mapper.deserialize[Team](teamJson)
```

## Explicit Metadata

When you already have a reflected type descriptor, pass it directly.

```scala
import reflect.macros.ReflectMacros.reflectType

val meta = reflectType[Account]
val json = mapper.serialize(account, meta)
val restored = mapper.deserialize[Account](json, meta)
```

## Arrays

```scala
import scala.scalajs.js

val arrayJson = js.Array(json)
val accounts = mapper.deserializeArray[Account](arrayJson, reflectType[Account])
```
