package owlet

import cats.Applicative
import cats.{ Functor }
import monix.execution.Scheduler.Implicits.global
import cats.syntax.apply._
import monix.reactive.Observable
import org.scalajs.dom._
import monix.reactive.subjects.Var
// import cats.free.Free

object Main {

  trait DOM

  // type UI[A] = Free[DOM, Owlet[A]]

  case class Owlet[A](nodes:List[Node], signal: Observable[A])

  implicit val functorOwlet = new Functor[Owlet] {
    def map[A,B](fa: Owlet[A])(f: A=>B) = {
      Owlet(fa.nodes, fa.signal.map(f))
    }
  }

  implicit val applicativeOwlet = new Applicative[Owlet] {
    def ap[A, B](ff: Owlet[A => B])(fa: Owlet[A]): Owlet[B] = Owlet( ff.nodes ++ fa.nodes, Observable.combineLatestMap2(ff.signal,fa.signal)(_(_)))
    def pure[A](a: A) = Owlet(Nil, Observable.pure[A](a))
  }

  def string(n: String, default: String = "") = {
    val input:html.Input = document.createElement("input").asInstanceOf[html.Input]
    input.name = n
    val state = Var(default)
    input.oninput = e => state := e.target.asInstanceOf[html.Input].value
    Owlet(List(input), state)
  }

  def main(args: scala.Array[String]): Unit = {
    val a = string("a")
    val b = string("b")
    val sum = (a,b).mapN(_+_)
    val app = document.querySelector("#app")
    sum.nodes.foreach(app.appendChild(_))
    sum.signal.foreach(println)
  }
}
