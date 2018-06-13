# Owlet

Owlet is a Typed Spreadsheet UI library for ScalaJS. It is built on top of [Monix](https://monix.io/) and [Typelevel Cats](https://typelevel.org/cats/)' Applicative syntax to combine predefined input fields to a reactive user interface, just like what you would done in spreadsheet. Owlet is inspired by the PureScript library [Flare](https://github.com/sharkdp/purescript-flare).

## Get Started

1. add dependency in your `build.sbt`
```
resolvers += "jitpack" at "https://jitpack.io"
libraryDependencies += "com.github.jcouyang" %%% "owlet" % "0.1.0"
```

2. now programming UI just like using spreadsheet
```scala
import us.oyanglul.owlet._
import DOM._
val a1 = number("a1", 1)
val a2 = number("a2", 2)
val a3 = number("a3", 3)
val sum = fx((a: List[Double]) => a.sum, List(a1, a2, a3))
val product = fx(((a: List[Double]) => a.product), List(a1, a2, a3))
render(a1 *> a2 *> a3 *> sum *> product, "#app")
```
