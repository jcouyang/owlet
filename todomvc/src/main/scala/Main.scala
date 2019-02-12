package us.oyanglul.owlettodomvc

import monix.reactive.Observable
import us.oyanglul.owlet._
import cats.implicits._
import monix.reactive.subjects.Var
import DOM._
import monix.execution.Scheduler.Implicits.global
import java.util.UUID
import org.scalajs.dom._

object Main {
  case class Todo(id: UUID, text: String, done: Boolean = false)
  case class Store(list: Vector[Todo], filter: Vector[Todo] => Vector[Todo])
  type Action = Store => Store

  object actions {
    def newTodo(txt: String): Action = { (store: Store) =>
      val todo = Todo(UUID.randomUUID, txt)
      console.log("creating todo", todo.toString())
      store.copy(list = todo +: store.list)
    }
    def deleteTodo(id: UUID): Action =
      (store: Store) => store.copy(list = store.list.filter(_.id != id))
    def toggleTodo(id: String, done: Boolean): Action =
      (store: Store) =>
        store.copy(list = store.list.map { todo =>
          println(todo)
          println(id)
          if (todo.id.toString == id)
            todo.copy(done = done)
          else
            todo
        })
    def allTodos: Action = (store: Store) => store.copy(filter = identity)
    def activeTodos: Action =
      (store: Store) => store.copy(filter = _.filter(!_.done))
    def completedTodos: Action =
      (store: Store) => store.copy(filter = _.filter(_.done))
  }

  def main(args: scala.Array[String]): Unit = {
    import actions._
    val events: Var[Action] = Var(identity)
    val reducedStore: Observable[Store] =
      events
        .scan(Store(Vector(), identity)) { (store, action) =>
          console.log("reduce", store.toString)
          action(store)
        }
        .share

    val todoInput = $.input[String]
      .modify { el =>
        el.autofocus = true
        el.onkeyup = e =>
          if (e.keyCode == 13) {
            println(s"creating new todo $el.value")
            events := newTodo(el.value)
            el.value = ""
          }
        el
      }(string("new-todo", ""))

    val todoHeader = div(h1("todos") &> todoInput, List("header"))

    val dataSource = Owlet(Owlet.emptyNode, reducedStore)

    def todoItem(todo: Todo): Owlet[(String, Boolean)] = {
      val checked = checkbox(todo.id.toString, todo.done, List("toggle")).map {
        case a @ (id, done) =>
          println(s"click:$id")
          if (todo.done != done) events := toggleTodo(id, done)
          a
      }
      val item = label(text(todo.text))
      val btn =
        button("", false, true, classNames = List("destroy")).map { del =>
          if (del) events := deleteTodo(todo.id)
          del
        }
      li(checked <& item <& btn, List("view"))
    }

    val todoList: Owlet[Vector[(String, Boolean)]] = div(
      ul(
        dataSource
          .flatMap { store =>
            console.log("store updated", store.toString())
            store.filter(store.list).parTraverse(todoItem)
          },
        List("todo-list")
      ),
      List("main")
    )

    val todoCount = todoList.flatMap { list =>
      span(
        text(s"${list.filter(!_._2).size} item left"),
        List("todo-count")
      )
    }

    val todoFilterAll = $.a[Action].modify { el =>
      val oldclick = el.onclick
      el.onclick = e => {
        el.className = "selected"
        oldclick(e)
      }
      el
    }(
      a(text("All"), allTodos)
    )
    val todoFilterActive = a(text("Active"), activeTodos)
    val todoFilterDone = a(text("Completed"), completedTodos)

    val todoFilters = (li(todoFilterAll) <+> li(todoFilterActive) <+>
      li(todoFilterDone)).map(events := _)

    val todoFooter =
      div(
        ul(
          li(todoCount) &> todoFilters,
          List("filters")
        ),
        List("footer")
      )

    render(
      div(todoHeader &> todoList &> todoFooter, List("todoapp")),
      "#application-container"
    ).runSyncStep
  }
}
