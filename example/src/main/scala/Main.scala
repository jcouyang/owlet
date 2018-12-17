package us.oyanglul.owletexample

import cats._
import us.oyanglul.owlet._
import cats.implicits._
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
      val pow = (baseInput, exponentInput).parMapN(math.pow)
      renderOutput(pow, "#example-1").runSyncStep
    }
    // Monoid
    {
      val helloText = string("hello", "Hello")
      val worldText = string("world", "World")
      renderOutput(helloText |+| " ".pure[Owlet] |+| worldText, "#example-2").runSyncStep

    }

    // Traverse
    {
      val sum = List(2, 13, 27, 42).parTraverse(int("n", _)).map(_.sum)
      renderOutput(sum, "#example-3").runSyncStep
    }

    // Select Box
    {
      val greeting = Map(
        "Chinese" -> "你好",
        "English" -> "Hello",
        "French" -> "Salut"
      )
      val selectBox = label(select("pierer", Var(greeting), "你好"), "Language")
      val hello = string("name", "Jichao")
      renderOutput(selectBox |+| " ".pure[Owlet] |+| hello, "#example-4").runSyncStep
    }

    // Checkbox
    {
      renderOutput(
        (boolean("a", false), boolean("b", true)).parMapN(_ && _),
        "#example-5"
      ).runSyncStep
    }

    // Buttons
    {
      val b = button("increament", 0, 1)
      renderOutput(b.fold(0)(_ + _), "#example-6").runSyncStep
    }

    // Adding items
    {
      val emptyList = const(List[String]()) _
      val addItem = (s: String) => List(s)
      val actions = Parallel.parAp(button("add", emptyList, addItem))(
        string(
          "add item",
          "Orange"
        )
      )
      val list = actions.fold(List[String]())(_ ::: _)
      renderOutput(list, "#example-7").runSyncStep
    }

    // Multiple Buttons
    {
      val intId = identity: Int => Int
      val inc = button("+ 1", intId, (x: Int) => x + 1)
      val dec = button("- 1", intId, (x: Int) => x - 1)
      val neg = button("+/-", intId, (x: Int) => -x)
      val reset = button("reset", intId, (x: Int) => 0)
      val buttons = inc <+> dec <+> neg <+> reset
      renderOutput(
        buttons.fold(0)((acc: Int, f: Int => Int) => f(acc)),
        "#example-8"
      ).runSyncStep
    }

    // List
    {
      val numOfItem = int("noi", 3)
      val items = numOfItem
        .flatMap(
          no => (0 to no).toList.parTraverse(i => string("inner", i.toString))
        )
      renderOutput(numOfItem &> items, "#example-13").runSyncStep
    }

    // Todo List
    {
      type Store = List[String]
      val actions: Var[Store => Store] = Var(identity)

      val newTodoInput = string("new-todo", "")
      val noop = (s: String) => identity: Store => Store
      val addItem = (s: String) => (store: Store) => s :: store
      val newTodo = Parallel
        .parAp(button("add", noop, addItem))(newTodoInput)
        .map(actions := _)

      val reduced = actions.scan(Nil: List[String]) { (store, action) =>
        action(store)
      }
      def createItem(content: String) = {
        val item = text(content, "todo-item")
        val empty = Monoid[Owlet[String]].empty
        val btn = button("delete", false, true)
        btn.flatMap { y =>
          if (y) {
            actions := ((store: Store) => store.filter(_ != content))
            empty
          } else li(item <& btn)
        }
      }
      val todos = Owlet(Nil, reduced).flatMap(_.parTraverse(createItem))

      render(newTodo &> todos, "#example-9").runSyncStep
    }

    // Spreadsheet like
    {
      val a1 = number("a1", 1)
      val a2 = number("a2", 2)
      val a3 = number("a3", 3)
      val sum = fx[Double, Double](_.sum, List(a1, a2, a3))
      val product = fx[Double, Double](_.product, List(a1, a2, a3, sum))
      render(a1 &> a2 &> a3 &> sum &> product, "#example-10").runSyncStep
    }

    // Scala Tags
    {
      val col = intSlider("col", 1, 20, 8)
      val row = intSlider("row", 1, 20, 8)
      import scalatags.Text.all._
      renderOutput((col, row).parMapN { (c, r) =>
        table((1 to r).map(ri => tr((1 to c).map(ci => td(s"$ri.$ci"))))).render
      }, "#example-11").runSyncStep
    }

    {
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
      renderOutput(selectBox |+| " ".pure[Owlet] |+| hello, "#example-12").runSyncStep
    }
  }
}
