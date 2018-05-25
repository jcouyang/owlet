package us.oyanglul

import cats.instances.string._
import cats.instances.list._
import cats.syntax.functor._
import cats.syntax.monoid._
import cats.syntax.applicative._
import cats.syntax.traverse._
import cats.syntax.semigroupk._
import cats.syntax.apply._
import monix.reactive.subjects.Var
import Function.const
import DOM._
import monix.execution.Scheduler.Implicits.global

object Main {
  def main(args: scala.Array[String]): Unit = {
    // Applicative
    {
      val baseInput = number("Base", 2.0)
      val exponentInput = number("Exponent", 10.0)
      val pow = (baseInput,exponentInput).mapN(math.pow)
      renderAppend(pow, "#example-1")
    }
    // Monoid
    {
      val helloText = string("hello", "Hello")
      val worldText = string("world", "World")
      renderAppend(
        helloText |+| " ".pure[Owlet] |+| worldText,
        "#example-2")
    }

    // Traverse
    {
      val sum = List(2, 13, 27, 42).traverse(int("n", _)).map(a => a.foldLeft(0)(_+_))
      renderAppend(sum, "#example-3")
    }

    // Select Box
    {
      val greeting = Map(
        "Chinese" -> "你好",
        "English" -> "Hello",
        "French" -> "Salut"
      )
      val selectBox = select("pierer", Var(greeting) , "你好")
      val hello = string("name", "Jichao")
      renderAppend(selectBox |+| " ".pure[Owlet] |+| hello, "#example-4")
    }

    // Checkbox
    {

    }

    // Buttons
    {
      val b = button("increament", 0, 1)
      renderAppend(b.fold(0)(_+_), "#example-6")
    }

    // Adding items
    {
      val emptyList = const(List[String]()) _
      val addItem = (s: String) => List(s)
      val actions = button("add",emptyList, addItem) <*> string("add item", "Orange")
      val list = actions.fold(List[String]())(_ ::: _)
      renderAppend(list, "#example-7")
    }

    // Multiple Buttons
    {
      val intId = identity: Int => Int
      val inc = button("+ 1", intId, (x:Int) => x + 1)
      val dec = button("- 1", intId, (x:Int) => x - 1)
      val neg = button("+/-", intId ,(x:Int) => -x)
      val reset = button("reset", intId, (x:Int) => 0)
      val buttons = inc <+> dec <+> neg <+> reset
      renderAppend(buttons.fold(0)((acc:Int, f:Int=>Int) => f(acc)), "#example-8")
    }

    // List
    {
      val emptyList = const(List[Owlet[Int]]()) _
      val addItem = (s: Int) => List(int("new item", s))
      val actions = button("add",emptyList, addItem) <*> int("add item", 0)
      val inputs = actions.fold(List[Owlet[Int]]())(_ ::: _)


      render(actions *> list(inputs), "#example-9")

    }

    // Spreadsheet like
    {
      val a1 = number("a1",1)
      val a2 = number("a2", 2)
      val a3 = number("a3", 3)
      val sum = fx(((a:List[Double]) => a.foldLeft(0d)(_+_)), List(a1,a2,a3))
      val product = fx(((a:List[Double]) => a.foldLeft(1d)(_*_)), List(a1,a2,a3))
      render(a1 *> a2 *> a3 *> sum *> product, "#example-10")
    }
  }
}
