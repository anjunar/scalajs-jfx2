addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.22.0")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.3.1")

libraryDependencies +=
  ("org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.1")
    .cross(CrossVersion.for3Use2_13)
    .exclude("org.scala-js", "scalajs-env-nodejs_2.13")
    .exclude("org.scala-js", "scalajs-logging_2.13")
    .exclude("org.scala-js", "scalajs-js-envs_2.13")