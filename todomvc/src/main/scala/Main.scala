package us.oyanglul.owlettodomvc

import us.oyanglul.owlet._
import cats.implicits._
import monix.reactive.subjects.Var
import DOM._
import Function.const
import monix.execution.Scheduler.Implicits.global
object Main {
  def main(args: scala.Array[String]): Unit = {

    val todos = Var(Nil): Var[List[Owlet[String]]]
    val actions = Var(identity): Var[List[Owlet[String]] => List[Owlet[String]]]
    val listOfTodos = todos.combineLatestMap(actions)((todo, f) => {
      val next = f(todo)
      todos := next
      next
    })

    val notAddItem = const(Nil) _
    val addItem = (s: String) => List(string("todo-item", s))

    val newTodo = div(string("new-todo", ""), Var("header"))
    val addNewTodo =
      (button("add", notAddItem, addItem) <*> newTodo)
        .map(t => actions := (a => a ::: t))
    addNewTodo.signal.foreach(println)

    val todoUl: Owlet[List[String]] = removableList(listOfTodos, actions)
    render(addNewTodo *> todoUl, "#application-container")
  }
}
