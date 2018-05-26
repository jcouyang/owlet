enablePlugins(ScalaJSPlugin)

name := "Owlet"

version := "0.1.0"

organization := "us.oyanglul"

scalaVersion in ThisBuild := "2.12.6"
scalacOptions in ThisBuild ++= Seq(
  "-encoding", "UTF-8",   // source files are in UTF-8
  "-deprecation",         // warn about use of deprecated APIs
  "-unchecked",           // warn about unchecked type parameters
  "-feature",             // warn about misused language features
  "-language:higherKinds",// allow higher kinded types without `import scala.language.higherKinds`
  "-Xlint",               // enable handy linter warnings
  // "-Xfatal-warnings",     // turn compiler warnings into errors
  "-Ypartial-unification" // allow the compiler to unify type constructors of different arities
)

libraryDependencies ++= Seq(
  "org.typelevel" %%% "cats-core" % "1.0.1",
  "org.typelevel" %%% "cats-free" % "1.0.1",
  "org.scala-js" %%% "scalajs-dom" % "0.9.2",
  "io.monix" %%% "monix" % "3.0.0-RC1",
  "org.scalatest" %%% "scalatest" % "3.0.3" % Test
)

lazy val docs = project.enablePlugins(MicrositesPlugin)
  .settings(
    micrositeName := "Owlet",
    micrositeDescription := "Lightweight, modular, composable UI library for Scala",
    micrositeAuthor := "Jichao Ouyang",
    micrositeHomepage := "https://oyanglul.us/owlet",
    micrositeOrganizationHomepage := "https://oyanglul.us",
    micrositeTwitter := "@oyanglulu",
    micrositeGithubOwner := "jcouyang",
    micrositeGithubRepo := "owlet",
    micrositeDocumentationUrl := "/api",
    micrositeGitterChannel := true,
    micrositeGitterChannelUrl := "jcouyang/owlet"
  )

scalaJSUseMainModuleInitializer := true

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")
