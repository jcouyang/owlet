---
layout: docs
title: Lens
section: en
position: 4
---

Most of component are predefined, but if you want to customize the element in Owlet, you can always use [Monocle](http://julien-truffaut.github.io/Monocle/)(or Lens, or Typed jQuery) to do so.

Owlet using Lens internally too to create new component based on existing component too.

E.g. we already know that `string` will create a `<input type=text/>` element and produce `String` type of data.

It's so easy to create `number` from it by using Lens

```scala
    $.input[String]
      .modify { el =>
        el.`type` = "number"
        el.step = "any"
        el
      }(string(name, default.toString))
      .map((x: String) => Try(x.toDouble).getOrElse(default))
```

- `$.input` give you a Lens to focus on the element in `string(name, default.toString)`
- by calling `.modify`, you got the ref of the actual element inside owlet string component
- you can do what ever you want, here we simply change it's type from `text` to `number`
- `.map((x: String) => Try(x.toDouble).getOrElse(default))` will convert the signal of string to signal of number

By using Lens you can far more flexibility that owlet builtin component can't provide. for more information ref

- [Todo MVC](https://github.com/jcouyang/owlet/blob/master/todomvc/src/main/scala/Main.scala)
- [API Doc](https://oyanglul.us/owlet/api/us/oyanglul/owlet/$$.html)
