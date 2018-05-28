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

<div id="example-5" ></div>

## Example 6: Buttons

<div id="example-6" ></div>

## Example 7: Adding items to a list

<div id="example-7" ></div>

## Example 8: Multiple buttons

<div id="example-8" ></div>

## Example 9: Resizable lists

<div id="example-9"></div>

## Example 10: Spreadsheet like

<div id="example-10"></div>

## Example 11: Scala Tag

<div id="example-11"></div>

<script src="demo/owlet-opt.js"></script>
