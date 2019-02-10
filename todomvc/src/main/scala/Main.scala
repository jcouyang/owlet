package us.oyanglul.owlettodomvc

import us.oyanglul.owlet._
import cats.implicits._
import monix.reactive.subjects.Var
import DOM._
import monix.execution.Scheduler.Implicits.global
import java.util.UUID

object Main {
  case class Todo(id: UUID, text: String)
  type Store = Vector[Todo]
  type Action = Store => Store

  object actions {
    def newTodo(txt: String): Action =
      (store: Store) => Todo(UUID.randomUUID, txt) +: store

    def deleteTodo(id: UUID): Action =
      (store: Store) => store.filter(_.id != id)

  }
  def main(args: scala.Array[String]): Unit = {
    import actions._

    val events: Var[Action] = Var(identity)
    val reducedStore = events.scan(Vector(): Store) { (store, action) =>
      action(store)
    }

    val todoInput = $.input
      .modify { el =>
        el.autofocus = true
        el.onkeyup = e =>
          if (e.keyCode == 13) {
            events := newTodo(el.value)
            el.value = ""
          }
        el
      }(string("new-todo", ""))

    val todoHeader = div(h1("todos") &> todoInput, Var(List("header")))

    def todoItem(todo: Todo): Owlet[(String, Boolean)] = {
      val checked = checkbox(todo.id.toString, false, List("toggle"))
      val item = label(text(todo.text))
      val btn =
        button("delete", false, true, classNames = List("destroy")).map { del =>
          if (del) events := deleteTodo(todo.id)
          del
        }
      li(checked <& item <& btn, Var(List("view")))
    }

    val todoList: Owlet[Vector[(String, Boolean)]] = div(
      ul(
        Owlet(Owlet.emptyNode, reducedStore)
          .flatMap(_.parTraverse(todoItem)),
        Var(List("todo-list"))
      ),
      Var(List("main"))
    )

    val todoCount = todoList.flatMap { list =>
      println(list)
      span(text(s"${list.filter(!_._2).size} item left"), List("todo-count"))
    }

    val todoFooter = todoCount
    render(
      div(todoHeader &> todoList &> todoFooter, Var(List("todoapp"))),
      "#application-container"
    ).runSyncStep
  }
}
