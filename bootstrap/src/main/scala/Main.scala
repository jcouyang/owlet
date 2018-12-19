package us.oyanglul.owletexample

import cats._
import us.oyanglul.owlet._
import cats.implicits._
import monix.reactive.subjects.Var
import DOM._
import monix.execution.Scheduler.Implicits.global
object Main {
  def main(args: scala.Array[String]): Unit = {
    val dismissAlert = button(
      name = "×",
      default = true,
      pressed = false,
      classNames = List("close")
    )
    val alert = h1("Alert") &>
      dismissAlert.flatMap { close =>
        if (close)
          div(
            text("you can close alert") &> dismissAlert,
            Var(List("alert", "alert-primary"))
          )
        else implicitly[MonoidK[Owlet]].empty
      }

    val badges = h1("Badges") &>
      span(
        text("Primary"),
        classNames = List("badge", "badge-primary")
      ) &>
      span(text("Secondary"), classNames = List("badge", "badge-secondary")) &>
      span(text("Success"), classNames = List("badge", "badge-success"))

    render(
      div(
        div(
          alert &> badges,
          Var(List("col-12"))
        ),
        Var(List("container-fluid"))
      ),
      "#app"
    ).runSyncStep
  }
}
