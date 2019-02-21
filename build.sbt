organization in ThisBuild := "us.oyanglul"

scalaVersion in ThisBuild := "2.12.8"
lazy val monocleVersion = "1.5.0-cats"
scalacOptions in ThisBuild ++= Seq(
  "-encoding", "UTF-8",   // source files are in UTF-8
  "-deprecation",         // warn about use of deprecated APIs
  "-unchecked",           // warn about unchecked type parameters
  "-feature",             // warn about misused language features
  "-language:higherKinds",// allow higher kinded types without `import scala.language.higherKinds`
  "-Xlint",               // enable handy linter warnings
  "-Xfatal-warnings",     // turn compiler warnings into errors
  "-Ypartial-unification" // allow the compiler to unify type constructors of different arities
)

lazy val owletVersion = "0.3.1"

lazy val owlet = project.in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "Owlet",
    version := owletVersion,
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/jcouyang/owlet"),
        "scm:git@github.com:jcouyang/owlet.git"
      )
    ),
    developers := List(
      Developer(
        id    = "jcouyang",
        name  = "Jichao Ouyang",
        email = "oyanglulu@gmail.com",
        url   = url("https://oyanglul.us")
      )
    ),
    licenses := List("MIT" -> new URL("https://opensource.org/licenses/MIT")),
    homepage := Some(url("https://oyanglul.us/owlet")),
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % "1.6.0",
      "com.github.julien-truffaut"  %%%  "monocle-core"    % monocleVersion,
            "com.github.julien-truffaut"  %%%  "monocle-macro"    % monocleVersion,
      "org.scala-js" %%% "scalajs-dom" % "0.9.2",
      "io.monix" %%% "monix-reactive" % "3.0.0-RC2",
      "org.scalatest" %%% "scalatest" % "3.0.3" % Test,
      "org.typelevel" %%% "cats-laws" % "1.0.1" % Test,
      "org.typelevel" %%% "cats-testkit" % "1.0.1"% Test
    ),
    jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    publishMavenStyle := true
  )

lazy val example = project.enablePlugins(ScalaJSPlugin).settings(
  scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "scalatags" % "0.6.7"
    )
).dependsOn(owlet)

lazy val bootstrap = project.enablePlugins(ScalaJSPlugin).settings(
  scalaJSUseMainModuleInitializer := true
).dependsOn(owlet)

lazy val todomvc = project.enablePlugins(ScalaJSPlugin).settings(
  scalaJSUseMainModuleInitializer := true
).dependsOn(owlet)

lazy val docs = project.enablePlugins(MicrositesPlugin)
  .settings(
    micrositeName := "Owlet",
    micrositeBaseUrl := "/owlet",
    micrositeDescription := "Typed Spreadsheet UI library for ScalaJS",
    micrositeAuthor := "Jichao Ouyang",
    micrositeHomepage := "https://oyanglul.us/owlet",
    micrositeOrganizationHomepage := "https://oyanglul.us",
    micrositeTwitter := "@oyanglulu",
    micrositeGithubOwner := "jcouyang",
    micrositeGithubRepo := "owlet",
    micrositeDocumentationUrl := "api",
    micrositeGitterChannel := true,
    micrositeGitterChannelUrl := "jcouyang/owlet",
    micrositeExtraMdFiles := Map(
      file("README.md") -> microsites.ExtraMdFileConfig(
        "index.md",
        "home",
        Map("title" -> "Home", "section" -> "home", "position" -> "0", "technologies" -> """
 - first: ["Scala", "Owlet components are completely written in Scala"]
 - second: ["Cats", "Owlet implements Cats typeclasses so you will get awesome cats syntax"]
 - third: ["Monix", "Owlet takes advantage from high performance Reactive lib Monix to build reactive UI component"]"""
        )
      )
    )
  )

target in Compile in doc := baseDirectory.value / "docs" / "src" / "main" / "tut" / "api"
scalafmtOnCompile in ThisBuild := true
addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")
