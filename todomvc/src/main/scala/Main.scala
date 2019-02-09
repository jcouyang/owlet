package us.oyanglul.owlettodomvc

import cats.kernel.Monoid
import us.oyanglul.owlet._
import cats.implicits._
import monix.reactive.subjects.Var
import DOM._
import monix.execution.Scheduler.Implicits.global
import java.util.UUID

object Main {
  def main(args: scala.Array[String]): Unit = {
    case class Todo(id: UUID, text: String)
    type Store = Vector[Todo]
    val actions: Var[Store => Store] = Var(identity)

    // add a new item
    val newItem = (txt: String) =>
      (store: Store) => Todo(UUID.randomUUID, txt) +: store

    val todoInput = $.input
      .modify { el =>
        el.onkeyup = e =>
          if (e.keyCode == 13) {
            actions := newItem(el.value)
            el.value = ""
          }
        el
      }(string("new-todo", ""))

    val todoHeader = div(todoInput, Var(List("header")))

    val reducedStore = actions.scan(Vector(): Store) { (store, action) =>
      action(store)
    }

    def deleteTodo(id: UUID) = (store: Store) => store.filter(_.id != id)

    def createItem(todo: Todo): Owlet[List[(String, Boolean)]] = {
      val checked = checkbox(todo.id.toString, false, List("toggle"))
      val item = label(text(todo.text))
      val empty = Monoid[Owlet[List[(String, Boolean)]]].empty
      val btn = button("delete", false, true, classNames = List("destroy"))
      btn.flatMap { y =>
        if (y) {
          actions := deleteTodo(todo.id)
          empty
        } else li(checked <& item <& btn, Var(List("view"))).map(List(_))
      }
    }

    val todoList = div(
      ul(
        Owlet(Owlet.emptyNode, reducedStore).flatMap(_.parTraverse(createItem)),
        Var(List("todo-list"))
      ),
      Var(List("main"))
    )

    render(
      div(todoHeader &> todoList, Var(List("todoapp"))),
      "#application-container"
    ).runSyncStep
  }
}
