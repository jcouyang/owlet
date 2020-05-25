addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.0.0")

libraryDependencies += "org.scala-js" %% "scalajs-env-nodejs" % "1.1.0"
libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"

addSbtPlugin("com.47deg"  % "sbt-microsites" % "0.8.0")
addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.6.0-RC1")
addSbtPlugin("ch.epfl.scala" % "sbt-release-early" % "2.1.1")
