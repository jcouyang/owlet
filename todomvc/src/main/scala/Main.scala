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

    val todoInput = string("new-todo", "")

    // do nothing
    val noop = (s: String) => identity: Store => Store
    // add a new item
    val newItem = (txt: String) =>
      (store: Store) => Todo(UUID.randomUUID, txt) +: store
    val todoAddButton = button("add", noop, newItem)
    val todoHeader = (todoAddButton <*> todoInput)
      .map(actions := _) <& todoAddButton

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

    val todoList = ul(
      Owlet(Owlet.emptyNode, reducedStore).flatMap(_.parTraverse(createItem)),
      Var(List("todo-list"))
    )

    render(todoHeader &> todoList, "#application-container").runSyncStep
  }
}
