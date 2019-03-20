# Owlet

Owlet is a Typed Spreadsheet UI library for ScalaJS. It is built on top of [Monix](https://monix.io/) and [Typelevel Cats](https://typelevel.org/cats/) to combine predefined input fields to a reactive user interface, just like what you would done in spreadsheet. Owlet is inspired by the PureScript library [Flare](https://github.com/sharkdp/purescript-flare).

[<img src=https://upload.wikimedia.org/wikipedia/commons/1/14/Imperial_Encyclopaedia_-_Animal_Kingdom_-_pic014_-_%E8%B2%93%E9%A0%AD%E9%B7%B9%E5%9C%96.svg width=50% />](https://zh.wikisource.org/wiki/%E6%AC%BD%E5%AE%9A%E5%8F%A4%E4%BB%8A%E5%9C%96%E6%9B%B8%E9%9B%86%E6%88%90/%E5%8D%9A%E7%89%A9%E5%BD%99%E7%B7%A8/%E7%A6%BD%E8%9F%B2%E5%85%B8)

## Get Started

### 1. add dependency in your `build.sbt`

#### Stable
[![Latest version](https://index.scala-lang.org/jcouyang/owlet/owlet/latest.svg)](https://index.scala-lang.org/jcouyang/owlet/owlet)

```
libraryDependencies += "us.oyanglul" %%% "owlet" % "<maven version>"
```

#### RC
[![](https://jitpack.io/v/jcouyang/owlet.svg)](https://jitpack.io/#jcouyang/owlet)

```
resolvers += "jitpack" at "https://jitpack.io"
libraryDependencies += "com.github.jcouyang" % "owlet" % "<jitpact version>"
```

### 2. Now programming UI is just like using spreadsheet

```scala
import us.oyanglul.owlet._
import DOM._
val a1 = number("a1", 1)
val a2 = number("a2", 2)
val a3 = number("a3", 3)
val sum = fx[List, Double, Double](_.sum, List(a1, a2, a3))
render(a1 &> a2 &> a3 &> sum, "#app")
```

or ![Cats Friendly Badge](https://typelevel.org/cats/img/cats-badge-tiny.png)

```scala
val a1 = number("a1", 1)
val a2 = number("a2", 2)
val a3 = number("a3", 3)
val sum = a1 |+| a2 |+| a3
renderOutput(sum, "#app")
```
### eh... Ready for 3D spreadsheet programming?
You know spreadsheet is 2D, when we have monad, it became 3D

!!!Monad Warning!!!

```scala
val numOfItem = int("noi", 3)
val items = numOfItem
  .flatMap(
    no => (0 to no).toList.parTraverse(i => string("inner", i.toString))
  )
```
- imagine that `numOfItem` lives in dimension (x=1, y=1, z=0)
- then `items` live in dimension (x=1,y=1,z=1)

you can render either `numOfItem` or `items` seperatly, for they live in diffenrent z axis (which means render `items` you won't able to see `numOfItem` even it's flatMap from there

but you can some how connect the dots with magic `&>`
```scala
renderOutput(numOfItem &> items, "#output")
```

Anyway, just keep in mind that monad ops `map` `ap` `flatMap`... will lift your z axis
`parMap` `parAp` `parXXX` instead, will keep them in the same z axis

## More...

- [Tutorial](https://oyanglul.us/owlet/Tutorial.html)
- Tweaking Owlet with [Lens](https://oyanglul.us/owlet/Lens.html)
- [Todo MVC](https://oyanglul.us/owlet/todomvc.html) Completed
- [API Doc](https://oyanglul.us/owlet/api)
