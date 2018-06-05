---
layout: docs
title: Examples
section: en
position: 3
---

# Owlet examples

## Example 1: Two number inputs

```scala
val baseInput = number("Base", 2.0)
val exponentInput = number("Exponent", 10.0)
val pow = (baseInput, exponentInput) mapN math.pow
```
<div id="example-1" ></div>

## Example 2: Semigroup instance

```scala
val helloText = string("hello", "Hello")
val worldText = string("world", "World")
val example2 = helloText |+| " ".pure[Owlet] |+| worldText
```
<div id="example-2" ></div>

## Example 3: Traverse

```scala
val sum = List(2, 13, 27, 42).traverse(int("n", _)).map(_.sum)
```
<div id="example-3" ></div>

## Example 4: Select box

```scala
val greeting = Map(
  "Chinese" -> "你好",
  "English" -> "Hello",
  "French" -> "Salut"
)
val selectBox = select("pierer", Var(greeting) , "你好")
val hello = string("name", "Jichao")
val example4 = selectBox |+| " ".pure[Owlet] |+| hello, "#example-4"
```

<div id="example-4" ></div>

## Example 5: Checkboxes

```scala
(boolean("a", false), boolean("b", true)).mapN(_ && _),
```

<div id="example-5" ></div>

## Example 6: Buttons

```scala
button("increament", 0, 1).fold(0)(_ + _)
```

<div id="example-6" ></div>

## Example 7: Adding items to a list
```
val emptyList = const(List[String]()) _
val addItem = (s: String) => List(s)
val actions = button("add", emptyList, addItem) <*>
              string("add item","Orange")
val list = actions.fold(List[String]())(_ ::: _)
```
<div id="example-7" ></div>

## Example 8: Multiple buttons
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

```
  val emptyList = const(List[Owlet[Int]]()) _
val addItem = (s: Int) => List(int("new item", s))
val actions = button("add", emptyList, addItem) <*> int("add item", 0)
val inputs = list(actions.fold(List[Owlet[Int]]())(_ ::: _))
val sum = fx((a: List[List[Int]]) => a.flatten.sum, List(inputs))
render(actions *> inputs *> sum, "#example-9")
```

## Example 9: Resizable lists
<div id="example-9"></div>

## Example 10: Spreadsheet like

```scala
val a1 = number("a1", 1)
val a2 = number("a2", 2)
val a3 = number("a3", 3)
val sum = fx((a: List[Double]) => a.sum, List(a1, a2, a3))
val product = fx(((a: List[Double]) => a.product), List(a1, a2, a3))
render(a1 *> a2 *> a3 *> sum *> product, "#example-10")
```

<div id="example-10"></div>

## Example 11: Scala Tag

```scala
val col = intSlider("col", 1, 20, 8)
val row = intSlider("row", 1, 20, 8)
import scalatags.Text.all._
renderOutput((col, row).mapN { (c, r) =>
  table((1 to r).map(ri => tr((1 to c).map(ci => td(s"$ri.$ci"))))).render
}, "#example-11")
```
<div id="example-11"></div>

<script src="demo/owlet-opt.js"></script>
