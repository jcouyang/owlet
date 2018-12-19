# Owlet

Owlet is a Typed Spreadsheet UI library for ScalaJS. It is built on top of [Monix](https://monix.io/) and [Typelevel Cats](https://typelevel.org/cats/)' Applicative syntax to combine predefined input fields to a reactive user interface, just like what you would done in spreadsheet. Owlet is inspired by the PureScript library [Flare](https://github.com/sharkdp/purescript-flare).

## Get Started

### add dependency in your `build.sbt`

#### Stable
[![Latest version](https://index.scala-lang.org/jcouyang/owlet/owlet/latest.svg)](https://index.scala-lang.org/<yjcouyang/owlet/owlet)

```
libraryDependencies += "us.oyanglul" %%% "owlet" % "0.1.5"
```

#### RC
[![](https://jitpack.io/v/jcouyang/owlet.svg)](https://jitpack.io/#jcouyang/owlet)

```
resolvers += "jitpack" at "https://jitpack.io"
libraryDependencies += "com.github.jcouyang" % "owlet" % "<jitpact version>"
```

2. now programming UI just like using spreadsheet
```scala
import us.oyanglul.owlet._
import DOM._
val a1 = number("a1", 1)
val a2 = number("a2", 2)
val a3 = number("a3", 3)
val sum     = fx[Double, Double](_.sum, List(a1, a2, a3))
val product = fx[Double, Double](_.product, List(a1, a2, a3))
render(a1 *> a2 *> a3 *> sum *> product, "#app")
```
