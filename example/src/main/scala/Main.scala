package us.oyanglul.owletexample

import us.oyanglul.owlet._
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
      renderOutput(pow, "#example-1")
    }
    // Monoid
    {
      val helloText = string("hello", "Hello")
      val worldText = string("world", "World")
      renderOutput(
        helloText |+| " ".pure[Owlet] |+| worldText,
        "#example-2")
    }

    // Traverse
    {
      val sum = List(2, 13, 27, 42).traverse(int("n", _)).map(_.sum)
      renderOutput(sum, "#example-3")
    }

    // Select Box
    {
      val greeting = Map(
        "Chinese" -> "你好",
        "English" -> "Hello",
        "French" -> "Salut"
      )
      val selectBox = label(select("pierer", Var(greeting) , "你好"), "Language")
      val hello = string("name", "Jichao")
      renderOutput(selectBox |+| " ".pure[Owlet] |+| hello, "#example-4")
    }

    // Checkbox
    {

    }

    // Buttons
    {
      val b = button("increament", 0, 1)
      renderOutput(b.fold(0)(_+_), "#example-6")
    }

    // Adding items
    {
      val emptyList = const(List[String]()) _
      val addItem = (s: String) => List(s)
      val actions = button("add",emptyList, addItem) <*> string("add item", "Orange")
      val list = actions.fold(List[String]())(_ ::: _)
      renderOutput(list, "#example-7")
    }

    // Multiple Buttons
    {
      val intId = identity: Int => Int
      val inc = button("+ 1", intId, (x:Int) => x + 1)
      val dec = button("- 1", intId, (x:Int) => x - 1)
      val neg = button("+/-", intId ,(x:Int) => -x)
      val reset = button("reset", intId, (x:Int) => 0)
      val buttons = inc <+> dec <+> neg <+> reset
      renderOutput(buttons.fold(0)((acc:Int, f:Int=>Int) => f(acc)), "#example-8")
    }

    // List
    {
      val emptyList = const(List[Owlet[Int]]()) _
      val addItem = (s: Int) => List(int("new item", s))
      val actions = button("add",emptyList, addItem) <*> int("add item", 0)
      val inputs = list(actions.fold(List[Owlet[Int]]())(_ ::: _))
      val sum = fx((a:List[List[Int]])=>a.flatten.sum, List(inputs))
    render(actions *> inputs *> sum, "#example-9")

    }

    // Spreadsheet like
    {
      val a1 = number("a1",1)
      val a2 = number("a2", 2)
      val a3 = number("a3", 3)
      val sum = fx((a:List[Double]) => a.sum, List(a1,a2,a3))
      val product = fx(((a:List[Double]) => a.product), List(a1,a2,a3))
      render(a1 *> a2 *> a3 *> sum *> product, "#example-10")
    }

    // Mustash
    {
      val col = intSlider("col", 1, 20, 4)
      val row = intSlider("row", 1,20,4)
      import scalatags.Text.all._
      renderOutput((col,row).mapN{(c, r) =>
        table((1 to r).map( ri =>
          tr((1 to c).map(ci =>
            td(s"$ri.$ci")
          ))
        )).render
      }, "#example-11")
    }
  }
}
