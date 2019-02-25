---
layout: docs
title: Tutorial
section: en
position: 3
---

## Example 1: Two number inputs

Imagine you want to build a web app to calculate a exponent.

the core business should be just as easy as:
```scala
val base = 2
val exponent = 10
val pow = math.pow(base, exponent)
```

You can simply translate code above into an interactive web app by using Owlet:

<iframe height="400px" frameborder="0" style="width: 100%" src="https://embed.scalafiddle.io/embed?sfid=QBXZHwB/1&layout=v53"></iframe>

How did I do this? It's Owlet meowgic:

`Owlet[_]` is instance of typeclass [Applicative](https://typelevel.org/cats/typeclasses/applicative.html)

So you can use `.mapN` which is a syntax from typeclass `Apply`

and `.mapN` is just like `.map`, but instead of map one Functor, you can map multiple Functors(unfortunatly, these functor need to be Apply as well). So here we just map over both `baseInput` and `exponentInput` using `math.pow`

> wait, but what the hell is `parMapN`?

Short Answer: It's parallel version of `mapN`

Long Story: it's from typeclass [`Parallel`](https://typelevel.org/cats/typeclasses/parallel.html), Owlet is actually a Monad, which you know, is running in sequence. but `base` and `exponent` don't depend on each other, they can run in parallel instead.

While Applicative can be parallel, so Owlet implement a Applicative version of `Owlet.Par`. `Owlet` and `Owlet.Par` is Isomorphic(means you have morphism back and forth, Owlet can be convert to Owlet.Par and backward).

Parallel is just the typeclass for it, since it help you to convert Monad to Applicative automatically by just adding `par` preffix in your method name.

So `parMapN` will know you want to use Applicative version of `mapN`, it will covert `Owlet` to `Owlet.Par`, map it and covert it back to `Owlet`.

## Example 2: Semigroup

`Owlet[A]` is also an instance of typeclass [Monoid](https://typelevel.org/cats/typeclasses/monoid.html)

Think about how you concat two string togather
```scala
val hello = "Hello"
val world = "World"
val helloworld = hellow + " " + world

```

Here is the web interactive version in Owlet:

<iframe height="400px" frameborder="0" style="width: 100%" src="https://embed.scalafiddle.io/embed?sfid=mDggvjd/1&layout=v50"></iframe>

`|+|` is the method from Semigroup so you can simply concat two value

## Example 3: Traverse

`Owlet[_]` is an instance of typeclass [Traverse](https://typelevel.org/cats/typeclasses/traverse.html)

which will be very useful when we need to create a `Owlet[List[A]]` from a `List[A]`

<iframe height="400px" frameborder="0" style="width: 100%" src="https://embed.scalafiddle.io/embed?sfid=CPTAwpq/1&layout=v50"></iframe>

> using `par*` again since those number aren't sequence.

## Example 4: Select box

Select box is just like a Map of data
```scala
val greeting = Map(
"Chinese" -> "你好",
"English" -> "Hello",
"French" -> "Salut"
)
val selectBox = label(select("pierer", Var(greeting), "你好"), "Language")
val hello = string("name", "Jichao")
val example4 = selectBox |+| " ".pure[Owlet] |+| hello
```

<div id="example-4" ></div>

## Example 5: Checkboxes

`checkbox` will generate a pair of value `(name, value)`

```scala
List(("a", false), ("b", true), ("c", false))
  .parTraverse { case (name, value) => checkbox(name, value) }
```

<div id="example-5" ></div>

`toggle` will generate exclusive value as radio

```scala
toggle("a", false, "b") <+> toggle("a", true, "a") <+> toggle("a", false, "c")
```

<div id="example-14" ></div>

## Example 6: Buttons

There are two effects from a `button`, button down will emit a value and button up will emit another

and you can simply `fold` those values into one

```scala
button("increament", 0, 1).fold(0)(_ + _)
```

<div id="example-6" ></div>

## Example 7: Adding items to a list
with `button`, it's easy to build a increamental list
```
val emptyList = const(List[String]()) _
val addItem = (s: String) => List(s)
val actions = button("add", emptyList, addItem) <&> string("add item","Orange"
val list = actions.fold(List[String]())(_ ::: _)
```
<div id="example-7" ></div>

## Example 8: Multiple buttons

`Owlet[_]`  is an instance of [MonoidK](https://typelevel.org/cats/typeclasses/monoidk.html) as well

with MonoidK method `<+>`, we can easily fold multi actions(like Elm) on our init value:
```scala
val intId = identity: Int => Int
val inc = button("+ 1", intId, (x: Int) => x + 1)
val dec = button("- 1", intId, (x: Int) => x - 1)
val neg = button("+/-", intId, (x: Int) => -x)
val reset = button("reset", intId, (x: Int) => 0)
val buttons = inc <+> dec <+> neg <+> reset
buttons.fold(0)((acc: Int, f: Int => Int) => f(acc))
)
```
<div id="example-8" ></div>

## Example 9: Resizable lists

Imagine how many lines of code you need to implement a todo list?

**JUST 20!!!**

``` scala
type Store = List[String]
val actions: Var[Store => Store] = Var(identity)

val newTodoInput = string("new-todo", "")
val noop = (s: String) => identity: Store => Store
val addItem = (s: String) => (store: Store) => s :: store
val newTodo = (button("add", noop, addItem) <&> newTodoInput)
  .map(actions := _)

val reduced = actions.scan(Nil: List[String]) { (store, action) =>
  action(store)
}
def createItem(content: String) = {
  val item = text(content)
  val empty = Monoid[Owlet[String]].empty
  val btn = button("delete", false, true)
  btn.flatMap { y =>
    if (y) {
      actions := ((store: Store) => store.filter(_ != content))
      empty
    } else li(item <& btn)
  }
}
val todos =
  Owlet(Owlet.emptyNode, reduced).flatMap(_.parTraverse(createItem))
```

<div id="example-9"></div>

## Example 10: Spreadsheet like

In spreadsheet it's very easy to do this:

![](https://www.evernote.com/l/ABcu84jUnGdFsaYpZSTMP1pLLIZRjBeo-ngB/image.png)

and we can do exactly the same thing programmatically

```scala
val a1 = number("a1", 1)
val a2 = number("a2", 2)
val a3 = number("a3", 3)
val sum = fx((a: List[Double]) => a.sum, List(a1, a2, a3))
val product = fx(((a: List[Double]) => a.product), List(a1, a2, a3, sum))
render(a1 &> a2 &> a3 &> sum &> product, "#example-10")
```

<div id="example-10"></div>

## Example 11: Scala Tag

easy to hook up with any template engine like Scala Tag reactively render complex UI

```scala
val col = intSlider("col", 1, 20, 8)
val row = intSlider("row", 1, 20, 8)
import scalatags.Text.all._
renderOutput((col, row).mapN { (c, r) =>
  table((1 to r).map(ri => tr((1 to c).map(ci => td(s"$ri.$ci"))))).render
}, "#example-11")
```
<div id="example-11"></div>

## Example 12: Monad
most important! Owlet is Monad!
```scala
      val greeting = Map(
        "Chinese" -> "你好",
        "English" -> "Hello",
        "French" -> "Salut"
      )
      val selectBox = label(select("pierer", Var(greeting), "你好"), "Language")
      val hello = for {
        selected <- selectBox
        towho <- if (selected == "你好") string("name", "继超")
        else string("name", "Jichao")
      } yield towho
      val example12 = selectBox |+| " ".pure[Owlet] |+| hello
```
<div id="example-12"></div>

## Example 13: List
```scala
      val numOfItem = int("noi", 3)
      val items = numOfItem
        .flatMap(
          no => (0 to no).toList.parTraverse(i => string("inner", i.toString))
        )
      val example13 = numOfItem &> items
```
<div id="example-13"></div>

<script src="demo/owlet-opt.js"></script>
