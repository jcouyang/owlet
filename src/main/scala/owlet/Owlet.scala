package us.oyanglul.owlet

import cats.{Applicative, Monoid, MonoidK, Monad, Functor}
import cats.syntax.monoid._
import monix.execution.Scheduler.Implicits.global
import monix.execution.Ack
import monix.reactive.Observable
import org.scalajs.dom._
import monix.reactive.subjects.Var
import scala.concurrent.Future
import scala.util.Try
import cats.syntax.traverse._
import cats.instances.list._
import cats.syntax.apply._

case class Owlet[A](nodes: Observable[List[Node]], signal: Observable[A]) {
  def fold[S](seed: => S)(op: (S, A) => S) = {
    Owlet(nodes, signal.scan(seed)(op))
  }
  def filter(b: A => Boolean) = {
    Owlet(nodes, signal.filter(b))
  }
}
object Monad {
  implicit def monadOwlet(
      implicit ft: Functor[Owlet],
      app: Applicative[Owlet]
  ) = new Monad[Owlet] {
    def flatMap[A, B](fa: Owlet[A])(f: A => Owlet[B]): Owlet[B] = {
      val owletOwlet: Owlet[Owlet[B]] = ft.map(fa)(f)
      Owlet(
        owletOwlet.signal.flatMap(_.nodes),
        owletOwlet.signal.flatMap(_.signal)
      )
    }

    def tailRecM[A, B](a: A)(f: A => Owlet[Either[A, B]]): Owlet[B] =
      f(a) match {
        case Owlet(node, signal) =>
          Owlet(node, signal.flatMap {
            case Left(next) => Observable.tailRecM(next)(c => f(c).signal)
            case Right(b)   => Observable.pure(b)
          })
      }
    def pure[A](a: A) = app.pure(a)
  }
}
object Owlet {
  implicit val functorOwlet = new Functor[Owlet] {
    def map[A, B](fa: Owlet[A])(f: A => B) = {
      Owlet(fa.nodes, fa.signal.map(f))
    }
  }

  implicit val applicativeOwlet = new Applicative[Owlet] {
    def ap[A, B](ff: Owlet[A => B])(fa: Owlet[A]): Owlet[B] =
      Owlet(
        Observable.combineLatestMap2(ff.nodes, fa.nodes)(_ |+| _),
        Observable.combineLatestMap2(ff.signal, fa.signal)(_(_))
      )
    def pure[A](a: A) = Owlet(Observable.pure(Nil), Observable.pure[A](a))
  }

  implicit val monoidKOwlet = new MonoidK[Owlet] {
    def empty[A]: Owlet[A] =
      Owlet(Observable.empty, Observable.empty)
    def combineK[A](x: Owlet[A], y: Owlet[A]): Owlet[A] =
      Owlet(
        Observable.combineLatestMap2(x.nodes, y.nodes)(_ |+| _),
        Observable.merge(x.signal, y.signal)
      )
  }

  implicit def monoidOwlet[A: Monoid] = new Monoid[Owlet[A]] {
    def combine(a: Owlet[A], b: Owlet[A]): Owlet[A] =
      Owlet(
        Observable.combineLatestMap2(a.nodes, b.nodes)(_ |+| _),
        Observable.combineLatestMap2(a.signal, b.signal)(_ |+| _)
      )
    def empty = Owlet(Observable.empty, Observable.empty)
  }
}

object DOM {
  // ==Input==
  def string(name: String, default: String): Owlet[String] = {
    val state = Var(default)
    val input = createInput(
      name,
      "text",
      default,
      e => state := e.target.asInstanceOf[html.Input].value
    )
    Owlet(Observable(List(input)), state)
  }

  def number(name: String, default: Double): Owlet[Double] = {
    val state = Var(default)
    val input = createInput(name, "number", default, e => {
      val value = e.target.asInstanceOf[html.Input].value
      Try(value.toDouble).foreach(state := _)
    })
    input.step = "any"
    Owlet(Observable(List(input)), state)
  }

  def numberSlider(
      name: String,
      min: Double,
      max: Double,
      default: Double
  ): Owlet[Double] = {
    val state = Var(default)
    val input = createInput(name, "range", default, e => {
      val value = e.target.asInstanceOf[html.Input].value
      Try(value.toDouble.toInt).foreach(state := _)
    })
    input.step = "any"
    input.min = min.toString
    input.max = max.toString
    Owlet(Observable(List(input)), state)
  }

  def int(name: String, default: Int): Owlet[Int] = {
    val state = Var(default)
    val input = createInput(name, "number", default, e => {
      val value = e.target.asInstanceOf[html.Input].value
      Try(value.toDouble.toInt).foreach(state := _)
    })
    Owlet(Observable(List(input)), state)
  }

  def boolean(name: String, default: Boolean): Owlet[Boolean] = {
    val sink = Var(default)
    val input = document.createElement("input").asInstanceOf[html.Input]
    input.`type` = "checkbox"
    input.name = name
    input.className = "owlet-input-" + normalize(name)
    input.checked = default
    input.onchange = e => sink := e.target.asInstanceOf[html.Input].checked
    Owlet(Observable(List(input)), sink)
  }

  def intSlider(
      name: String,
      min: Int,
      max: Int,
      default: Int
  ): Owlet[Int] = {
    val state = Var(default)
    val input = createInput(name, "range", default, e => {
      val value = e.target.asInstanceOf[html.Input].value
      Try(value.toDouble.toInt).foreach(state := _)
    })
    input.step = "1"
    input.min = min.toString
    input.max = max.toString
    Owlet(Observable(List(input)), state)
  }

  private def cleanAppend[A](inner: Owlet[A], outer: Node) = {
    inner.nodes.foreach { n =>
      while (outer.lastChild != null) {
        outer.removeChild(outer.lastChild)
      }
      n.foreach(outer.appendChild)
    }
  }
  private def createInput[A](
      n: String,
      t: String,
      default: A,
      transform: Event => Unit,
  ) = {
    val input: html.Input =
      document.createElement("input").asInstanceOf[html.Input]
    input.`type` = t
    input.name = n
    input.className = "owlet-input " + normalize(n)
    input.defaultValue = default.toString
    input.oninput = e => transform(e)
    input
  }

  /**
    * Select
    */
  def select(
      name: String,
      source: Observable[Map[String, String]],
      default: String
  ): Owlet[String] = {
    val el = document.createElement("select").asInstanceOf[html.Select]
    source.foreach(options => {
      el.innerHTML = options.map { (kv: (String, String)) =>
        val op = document.createElement("option").asInstanceOf[html.Option]
        op.text = kv._1
        op.value = kv._2
        op.defaultSelected = (kv._1 == default)
        op.outerHTML
      }.mkString
    })
    val sink = Var(default)
    el.onchange = e => sink := e.target.asInstanceOf[html.Select].value
    Owlet(Observable.pure(List(el)), sink)
  }

  /**
    * button emit `default` value immediatly and emit `pressed` value every time it's clicked
    */
  def button[A](name: String, default: A, pressed: A) = {
    val el = document.createElement("button").asInstanceOf[html.Button]
    el.appendChild(document.createTextNode(name))
    val sink = Var(default)
    el.onmousedown = _ => sink := pressed
    el.onmouseup = _ => sink := default
    Owlet(Observable.pure(List(el)), sink)
  }

  /**
    * Container
    *
    * wrap nodes in `Owlet` into container element `div`, `label` etc
    * style of div can reactive from a stream of `className`
    */
  def div[A](
      inner: Owlet[A],
      className: Observable[Seq[String]] = Observable.empty,
      id: Option[String] = None
  ): Owlet[A] = {
    val el = document.createElement("div").asInstanceOf[html.Div]
    id.map(el.id = _)
    className.foreach(c => el.className = c.mkString(" "))
    cleanAppend(inner, el)
    Owlet(Observable.pure(List(el)), inner.signal)
  }

  def label[A](inner: Owlet[A], name: String): Owlet[A] = {
    val el = document.createElement("label").asInstanceOf[html.Label]
    el.appendChild(document.createTextNode(name))
    cleanAppend(inner, el)
    Owlet(Observable.pure(List(el)), inner.signal)
  }

  /** ==Output==
    *
    */
  def list[A](items: Owlet[List[Owlet[A]]]) = {
    val sink = Var(List[A]())
    val ul: html.UList = document.createElement("ul").asInstanceOf[html.UList]
    items.signal.foreach { owlets =>
      while (ul.lastChild != null) {
        ul.removeChild(ul.lastChild)
      }
      owlets.foreach(cleanAppend(_, ul))
      owlets.sequence.signal.foreach(sink := _)
    }
    Owlet(Observable.pure(List(ul)), sink)
  }

  def removableList[A](
      items: Observable[List[Owlet[A]]],
      actions: Var[List[Owlet[String]] => List[Owlet[String]]]
  ): Owlet[List[A]] = {
    val sink = Var(List[A]())
    val ul: html.UList = document.createElement("ul").asInstanceOf[html.UList]
    items.foreach { owlets =>
      while (ul.lastChild != null) {
        ul.removeChild(ul.lastChild)
      }
      owlets.foreach { owlet =>
        val li = document.createElement("li").asInstanceOf[html.LI]
        owlet.nodes.foreach {
          _.foreach { node =>
            val el = document.createElement("div").asInstanceOf[html.Div]
            el.appendChild(node)
            val removeButton =
              document.createElement("button").asInstanceOf[html.Button]
            removeButton.appendChild(document.createTextNode("x"))
            removeButton.onclick = _ => {
              actions := (todos => todos diff List(owlet))
              ul.removeChild(li)
            }
            el.appendChild(removeButton)
            li.appendChild(el)
          }
        }
        ul.appendChild(li)
      }
      owlets.sequence.signal.foreach(sink := _)
    }
    Owlet(Observable.pure(List(ul)), sink)
  }

  /** Spreadsheet like fx
    * create a new Owlet with existing Owlets with a formula
    */
  def fx[A, B](formula: List[A] => B, input: List[Owlet[A]]): Owlet[B] = {
    val div: html.Div = document.createElement("div").asInstanceOf[html.Div]
    val sink = input.sequence.signal.map(formula)
    sink.foreach(a => div.textContent = a.toString)
    Owlet(Observable.pure(List(div)), sink)
  }

  def output[A](
      input: Owlet[A],
      classNames: Observable[Seq[String]] = Var(Nil)
  ) = {
    val div = document.createElement("div").asInstanceOf[html.Div]
    classNames.foreach(c => div.className = c.mkString(" "))
    div.className += " owlet-output"
    Owlet(input.signal.map { e =>
      div.innerHTML = e.toString
      List(div)
    }, input.signal)
  }

  /**
    * Render
    */
  def render[A](owlet: Owlet[A], selector: String) = {
    owlet.signal.subscribe
    owlet.nodes.subscribe
    cleanAppend(owlet, document.querySelector(selector))
  }

  def renderOutput[A](owlet: Owlet[A], selector: String) = {
    render(owlet *> output(owlet), selector)
  }

  private def normalize(s: String) = s.replaceAll(" ", "-").toLowerCase
}
