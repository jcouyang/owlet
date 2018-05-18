package owlet

import cats.{ Applicative, Monoid }
import cats.syntax.monoid._
import cats.{ Functor }
import monix.execution.Scheduler.Implicits.global
import cats.syntax.apply._
import monix.reactive.Observable
import org.scalajs.dom._
import monix.reactive.subjects.Var
import scala.util.Try
import cats.instances.string._
import cats.instances.list._
import cats.syntax.functor._
import cats.syntax.applicative._
import cats.syntax.traverse._

object Main {

  case class Owlet[A](nodes:List[Node], signal: Observable[A]) {
    def fold[S](seed: =>S)(op:(S,A)=>S) = {
      Owlet(nodes, signal.scan(seed)(op))
    }
  }

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
    val input = createInput(n, "text", default, e => state := e.target.asInstanceOf[html.Input].value)
    Owlet(List(input), state)
  }

  def number(n: String, default: Double = 0d) = {
    val state = Var(default)
    val input = createInput(n, "number", default, e => {
      val value = e.target.asInstanceOf[html.Input].value
      Try(value.toDouble).foreach(state := _)
    })
    input.step = "any"
    Owlet(List(input), state)
  }

  def int(n: String, default: Int = 0) = {
    val state = Var(default)
    val input = createInput(n, "number", default, e => {
      val value = e.target.asInstanceOf[html.Input].value
      Try(value.toDouble.toInt).foreach(state := _)
    })

    Owlet(List(input), state)
  }

  def createInput[A](n: String, t: String, default: A, transform: Event=>Unit) = {
    val input:html.Input = document.createElement("input").asInstanceOf[html.Input]
    input.`type` = t
    input.name = n
    input.defaultValue = default.toString
    input.oninput = e => transform(e)
    input
  }

  /**
  * Select
  */
  def select(name: String, source: Observable[Map[String, String]], default: String):Owlet[String] = {
    val el = document.createElement("select").asInstanceOf[html.Select]
    source.foreach(options => {
      el.innerHTML = options.map{ (kv:(String, String)) =>
        val op = document.createElement("option").asInstanceOf[html.Option]
        op.text = kv._1
        op.value = kv._2
        op.defaultSelected = (kv._1 == default)
        op.outerHTML
      }.mkString
    })
    val sink = Var(default)
    el.onchange = e => sink := e.target.asInstanceOf[html.Select].value
    Owlet(List(el), sink)
  }

  /**
  * button emit `default` value immediatly and emit `pressed` value every time it's clicked
  */
  def button[A](name: String, default: A, pressed: A) = {
    val el = document.createElement("button").asInstanceOf[html.Button]
    el.appendChild(document.createTextNode(name))
    val sink = Var(default)
    el.onclick = _ => sink := pressed
    Owlet(List(el), sink)
  }
  /**
  * Output Owlet Component
  */
  def output[A](input: Owlet[A], classNames: Observable[List[String]] = Var(Nil)) ={
    val div = document.createElement("div").asInstanceOf[html.Div]
    classNames.foreach(c=>div.className = c.mkString(" "))
    input.signal.foreach(v => div.innerHTML = v.toString)
    input.nodes :+ div
  }

  def renderAppend[A](owlet: Owlet[A], selector: String) = {
    output(owlet)
      .foreach(document.querySelector(selector).appendChild(_))
  }

  def main(args: scala.Array[String]): Unit = {
    // Applicative
    {
      val baseInput = number("Base", 2.0)
      val exponentInput = number("Exponent", 10.0)
      val pow = (baseInput,exponentInput).mapN(math.pow)
      renderAppend(pow, "#example-1")
    }
    // Monoid
    {
      val helloText = string("hello", "Hello")
      val worldText = string("world", "World")
      renderAppend(
        helloText |+| " ".pure[Owlet] |+| worldText,
        "#example-2")
    }

    // Traverse
    {
      val sum = List(2, 13, 27, 42).traverse(int("n", _)).map(a => a.foldLeft(0)(_+_))
      renderAppend(sum, "#example-3")
    }

    // Select Box
    {
      val greeting = Map(
        "Chinese" -> "你好",
        "English" -> "Hello",
        "French" -> "Salut"
      )
      val selectBox = select("pierer", Var(greeting) , "你好")
      val hello = string("name", "Jichao")
      renderAppend(selectBox |+| " ".pure[Owlet] |+| hello, "#example-4")
    }

    // Checkbox
    {

    }

    // Buttons
    {
      val b = button("increament", 0, 1)
      renderAppend(b.fold(0)(_+_), "#example-6")
    }
  }
}
