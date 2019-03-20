package us.oyanglul.owlet

import cats.{Eval, Later, Show, Traverse}
import monix.eval.Task
import monix.execution.{Ack, Cancelable}
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import monix.reactive.OverflowStrategy.Unbounded
import monix.reactive.subjects.PublishSubject
import org.scalajs.dom._
import monix.reactive.subjects.Var
import org.scalajs.dom.raw.HTMLElement

import scala.util.Try
import cats.instances.list._
import cats.syntax.traverse._
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.show._
import cats.syntax.parallel._
import monix.execution.cancelables.SingleAssignCancelable

object DOM {
  // ==Input==
  def string(
      name: String,
      default: String,
      classNames: Seq[String] = Nil
  ): Owlet[String] = {
    val node = Later {
      val input: html.Input =
        document.createElement("input").asInstanceOf[html.Input]
      input.name = name
      input.`type` = "text"
      input.className = classNames.mkString(" ")
      input.defaultValue = default.toString
      input
    }
    Owlet(
      node.map(List(_)),
      eventListener(node, "input")
        .map { _.target.asInstanceOf[html.Input].value }
        .prepend(default)
    )
  }

  def number(name: String, default: Double): Owlet[Double] = {
    $.input[String]
      .modify { el =>
        el.`type` = "number"
        el.step = "any"
        el
      }(string(name, default.toString))
      .map((x: String) => Try(x.toDouble).getOrElse(default))
  }

  def numberSlider(
      name: String,
      min: Double,
      max: Double,
      default: Double
  ): Owlet[Double] = {
    $.input[String]
      .modify { el =>
        el.`type` = "range"
        el.step = "any"
        el.min = min.toString
        el.max = max.toString
        el
      }(string(name, default.toString))
      .map(x => Try(x.toDouble).getOrElse(default))
  }

  def int(name: String, default: Int): Owlet[Int] =
    $.input[Double]
      .modify(el => {
        el.step = "1"
        el
      })(number(name, default))
      .map(_.toInt)

  def checkbox(
      name: String,
      default: Boolean,
      classNames: List[String] = Nil
  ): Owlet[(String, Boolean)] = {
    val node = Later {
      val input: html.Input =
        document.createElement("input").asInstanceOf[html.Input]
      input.name = name
      input.`type` = "checkbox"
      input.checked = default
      input.className = classNames.mkString(" ")
      input.defaultValue = default.toString
      input
    }
    Owlet(
      node.map(List(_)),
      eventListener(node, "change")
        .map { e =>
          val el = e.target.asInstanceOf[html.Input]
          (el.name, el.checked)
        }
        .prepend((name, default))
    )
  }

  def toggle(
      name: String,
      default: Boolean = false,
      value: String = ""
  ): Owlet[String] =
    $.input[String].modify { el =>
      el.`type` = "radio"
      el.checked = default
      el
    }(string(name, value))

  def intSlider(
      name: String,
      min: Int,
      max: Int,
      default: Int
  ): Owlet[Int] =
    $.input[Double]
      .modify { el =>
        el.step = "1"
        el
      }(numberSlider(name, min, max, default))
      .map(_.toInt)

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
    val signal = Var(default)
    el.onchange = e => signal := e.target.asInstanceOf[html.Select].value
    Owlet(Later(List(el)), signal)
  }

  /**
    * button emit `default` value immediatly and emit `pressed` value every time it's clicked
    */
  def button[A](
      name: String,
      default: A,
      pressed: A,
      classNames: Seq[String] = Nil
  ) = {
    val signal = Var(default)
    val node = Later {
      val el = document.createElement("button").asInstanceOf[html.Button]
      el.appendChild(document.createTextNode(name))
      el.className = classNames.mkString(" ")
      el.onmousedown = _ => signal := pressed
      el.onmouseup = _ => signal := default
      el
    }
    Owlet(node.map(List(_)), signal)
  }

  def a[A](
      child: Owlet[_],
      pressed: A,
      classNames: Seq[String] = Nil,
      href: String = "#"
  ) = {
    val signal = PublishSubject[A]()
    val node = child.nodes.flatMap(
      c =>
        Later {
          val el = document.createElement("a").asInstanceOf[html.Anchor]
          c.foreach(el.appendChild)
          el.className = classNames.mkString(" ")
          el.onclick = _ => signal onNext pressed
          el.href = href
          el
        }
    )
    Owlet(node.map(List(_)), signal)
  }

  /**
    * Container
    *
    * wrap nodes in `Owlet` into container element `div`, `label` etc
    * style of div can reactive from a stream of `className`
    */
  private def createContainer[A, Tag <: HTMLElement](
      tag: String,
      inner: Owlet[A],
      className: Seq[String],
      id: Option[String]
  ): Owlet[A] = {
    val wrapped = inner.nodes.map { nodes =>
      val el = document.createElement(tag).asInstanceOf[Tag]
      id.map(el.id = _)
      el.className = className.mkString(" ")
      nodes.foreach(el.appendChild)
      List(el)
    }
    Owlet(wrapped, inner.signal)
  }

  def div[A](
      inner: Owlet[A],
      className: Seq[String] = Nil,
      id: Option[String] = None
  ) = {
    createContainer[A, html.Div]("div", inner, className, id)
  }

  def span[A](
      inner: Owlet[A],
      classNames: Seq[String] = Nil,
      id: Option[String] = None
  ) = {
    createContainer[A, html.Span]("span", inner, classNames, id)
  }

  def h1(
      content: String,
      classNames: Seq[String] = Nil,
      id: Option[String] = None
  ) =
    createContainer[String, html.Heading](
      "h1",
      text(content),
      classNames,
      id
    )

  def ul[A](
      inner: Owlet[A],
      className: Seq[String] = Nil,
      id: Option[String] = None
  ) = {
    createContainer[A, html.UList]("ul", inner, className, id)
  }

  def li[A](
      inner: Owlet[A],
      className: Seq[String] = Nil,
      id: Option[String] = None
  ) = {
    createContainer[A, html.LI]("li", inner, className, id)
  }

  def label[A](
      inner: Owlet[A],
      text: String = "",
      className: Seq[String] = Nil,
      id: Option[String] = None
  ): Owlet[A] = {
    createContainer[A, html.Label]("label", inner, className, id)
  }

  def text(content: String): Owlet[String] = {
    Owlet(Later(List(document.createTextNode(content))), Observable(content))
  }

  /** Spreadsheet like fx
    * Create a new Owlet with existing Owlets with a formula.
    * The formula can be from any instance of Traverse such as List
    */
  def fx[F[_]: Traverse, A, B: Show](
      formula: F[A] => B,
      input: F[Owlet[A]]
  ): Owlet[B] = {
    Traverse[F].sequence(input).map(formula)
  }

  def output[A: Show](
      input: Owlet[A],
      classNames: Seq[String] = Nil,
  ) = {
    div(input.flatMap(o => text(o.show)), classNames)
  }

  def unsafeOutput[A: Show](
      input: Owlet[A],
      classNames: Seq[String] = Nil,
  ) = {
    input.flatMap { content =>
      val node = Later {
        val el = document.createElement("div").asInstanceOf[html.Div]
        el.innerHTML = content.show
        el.className = classNames.mkString(" ")
        el
      }
      Owlet(node.map(List(_)), input.signal)
    }
  }

  /**
    * Render
    */
  def render[A](owlet: Owlet[A], selector: String): Task[Unit] =
    render(owlet, document.querySelector(selector))

  def render[A](owlet: Owlet[A], elm: Element): Task[Unit] =
    for {
      _ <- Task {
        owlet.nodes.value
          .foreach(elm.appendChild)
      }
      _ <- Task(owlet.signal.subscribe)
    } yield ()

  def renderOutput[A: Show](owlet: Owlet[A], selector: String) =
    render(owlet &> output(owlet), selector)

  def unsafeRenderOutput[A: Show](owlet: Owlet[A], selector: String) =
    render(owlet &> unsafeOutput(owlet), selector)

  private def eventListener(
      target: Eval[EventTarget],
      event: String
  ): Observable[Event] =
    (Observable
      .create[Event](Unbounded) { subscriber =>
        val c = SingleAssignCancelable()
        val f: scalajs.js.Function1[Event, Ack] =
          (e: Event) => {
            subscriber.onNext(e).syncOnStopOrFailure(_ => c.cancel())
          }
        target.value.addEventListener(event, f)
        c := Cancelable(() => target.value.removeEventListener(event, f))
      })
      .share
}
