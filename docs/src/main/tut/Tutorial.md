---
layout: docs
title: Tutorial
section: en
position: 3
---

## Example 1: Two number inputs

Imagine you want to build a web app to calculate an exponent.

The core business should be just as easy as:
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

and `.mapN` is just like `.map`, but instead of map one Functor, you can map multiple Functors(unfortunately, these functors need to be Apply as well). So here we just map over both `baseInput` and `exponentInput` using `math.pow`

> wait, but what the hell is `parMapN`?

Short Answer: It's parallel version of `mapN`

Long Story: it's from typeclass [`Parallel`](https://typelevel.org/cats/typeclasses/parallel.html), Owlet is actually a Monad, which you know, is running in sequence. But `base` and `exponent` don't depend on each other, they can run in parallel instead.

While Applicative can be parallel, so Owlet implement an Applicative version of `Owlet.Par`.  `Owlet` and `Owlet.Par` is Isomorphic(means you have morphism back and forth, Owlet can be converted to Owlet.Par and backward).

Parallel is just the typeclass for it, since it help you to convert Monad to Applicative automatically by just adding `par` prefix in your method name.

So `parMapN` will know you want to use Applicative version of `mapN`, it will covert `Owlet` to `Owlet.Par`, map it and covert it back to `Owlet`.

## Example 2: Semigroup

`Owlet[A]` is also an instance of typeclass [Monoid](https://typelevel.org/cats/typeclasses/monoid.html)

Think about how you concat two string together
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

Select box is just like a Map of data,  it will emit value you select

<iframe height="400px" frameborder="0" style="width: 100%" src="https://embed.scalafiddle.io/embed?sfid=mHNvMx3/1&layout=v80"></iframe>

## Example 5: Checkboxes

`checkbox` will generate a pair of value `(name, value)`

so if we traverse a list of checkboxes, then we have a list of `(name, value)`

<iframe height="400px" frameborder="0" style="width: 100%" src="https://embed.scalafiddle.io/embed?sfid=5tySTHF/1&layout=v50"></iframe>

`toggle` group will generate exclusive value

<iframe height="400px" frameborder="0" style="width: 100%" src="https://embed.scalafiddle.io/embed?sfid=sP6DxSS/2&layout=v50"></iframe>

## Example 6: Buttons

There are two effects from a `button`, button down will emit a value and button up will emit another

and you can simply `fold` those values into one

<iframe height="400px" frameborder="0" style="width: 100%" src="https://embed.scalafiddle.io/embed?sfid=9FWc6vK/1&layout=v50"></iframe>

## Example 7: Adding items to a list

with `button`, it's easy to build an incremental list

<iframe height="400px" frameborder="0" style="width: 100%" src="https://embed.scalafiddle.io/embed?sfid=akI5Rs9/1&layout=v50"></iframe>

## Example 8: Multiple buttons

`Owlet[_]`  is an instance of [MonoidK](https://typelevel.org/cats/typeclasses/monoidk.html) as well

with MonoidK method `<+>`, we can easily fold multi actions(Elm style) on our init value:

<iframe height="400px" frameborder="0" style="width: 100%" src="https://embed.scalafiddle.io/embed?sfid=Ku1mN4c/1&layout=v64"></iframe>

## Example 9: Resizable lists

Imagine how many lines of code you need to implement a simple todo list?

<iframe height="400px" frameborder="0" style="width: 100%" src="https://embed.scalafiddle.io/embed?sfid=Q1VuNGK/1&layout=h65"></iframe>

## Example 10: Spreadsheet like

In spreadsheet it's very easy to do this:

![](https://www.evernote.com/l/ABcu84jUnGdFsaYpZSTMP1pLLIZRjBeo-ngB/image.png)

and we can do exactly the same thing programmatically

<iframe height="400px" frameborder="0" style="width: 100%" src="https://embed.scalafiddle.io/embed?sfid=xgdEWFA/1&layout=v66"></iframe>

## Example 11: Scala Tag

easy to hook up with any template engine like Scala Tag reactively render complex UI

<iframe height="500px" frameborder="0" style="width: 100%" src="https://embed.scalafiddle.io/embed?sfid=iZ2iEnY/3&layout=v75"></iframe>

## Example 12: Monad

most important! Owlet is Monad!

<iframe height="400px" frameborder="0" style="width: 100%" src="https://embed.scalafiddle.io/embed?sfid=03HPHDz/1&layout=h74"></iframe>

## Example 13: Resizable List

<iframe height="400px" frameborder="0" style="width: 100%" src="https://embed.scalafiddle.io/embed?sfid=KGwrRdd/1&layout=v56"></iframe>
