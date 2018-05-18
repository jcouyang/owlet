package owlet

import cats.{ Applicative, Monoid }
import cats.syntax.monoid._
import cats.{ Functor }
import monix.execution.Scheduler.Implicits.global
import cats.syntax.apply._
import cats.syntax.functor._
import monix.reactive.Observable
import org.scalajs.dom._
import monix.reactive.subjects.Var
import scala.util.Try
import cats.instances.string._
// import cats.free.Free

object Main {

  trait DOM

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

  implicit def monoidOwlet[A:Monoid] = new Monoid[Owlet[A]] {
    def combine(a: Owlet[A], b:Owlet[A]):Owlet[A] = Owlet(a.nodes ++ b.nodes, Observable.combineLatestMap2(a.signal, b.signal)(_ |+| _))
    def empty = Owlet(List[Node](), Observable.empty)
  }

  // Input
  def string(n: String, default: String = "") = {
    val state = Var(default)
    val input = createInput(n, "text", e => state := e.target.asInstanceOf[html.Input].value)
    Owlet(List(input), state)
  }

  def number(n: String, default: Double = 0d) = {
    val state = Var(default)
    val input = createInput(n, "number", e => {
      val value = e.target.asInstanceOf[html.Input].value
      Try(value.toDouble).foreach(state := _)
    })
    Owlet(List(input), state)
  }

  def int(n: String, default: Int = 0) = {
    val state = Var(default)
    val input = createInput(n, "number", e => {
      val value = e.target.asInstanceOf[html.Input].value
      Try(value.toDouble.toInt).foreach(state := _)
    })
    Owlet(List(input), state)
  }

  def createInput[A](n: String, t: String, transform: Event=>Unit) = {
    val input:html.Input = document.createElement("input").asInstanceOf[html.Input]
    input.`type` = t
    input.name = n
    input.oninput = e => transform(e)
    input
  }

  // Select
  def select(name: String, source: Observable[Map[String, String]], default: String):Owlet[String] = {
    val el = document.createElement("select").asInstanceOf[html.Select]
    source.foreach(options => {
      el.innerHTML = options.map{ (kv:(String, String)) =>
        val op = document.createElement("option").asInstanceOf[html.Option]
        op.text = kv._2
        op.value = kv._1
        op.defaultSelected = (kv._1 == default)
        op.outerHTML
      }.mkString
    })
    val sink = Var(default)
    el.onchange = e => sink := e.target.asInstanceOf[html.Select].value
    Owlet(List(el), sink)
  }
  // Output
  def output(id: String, classNames: Observable[List[String]], input: Owlet[String]) ={
    val div = document.createElement("div").asInstanceOf[html.Div]
    div.id = "owlet-output-" + id
    classNames.foreach(c=>div.className = c.mkString(" "))
    input.signal.foreach(div.innerHTML = _)
    input.nodes :+ div
  }

  def main(args: scala.Array[String]): Unit = {
    val a = number("a")
    val b = int("b")
    val c = select("c", b.signal.map(_.toString).map(x=>Map(x->x)), "b")
    val sum = (a,b).mapN(_+_)
    val app = document.querySelector("#app")
    output("asdf", Var(List("yay")), b.map(_.toString) |+| c).foreach(app.appendChild)
  }
}
