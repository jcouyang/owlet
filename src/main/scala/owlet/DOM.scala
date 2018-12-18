package us.oyanglul.owlet

import cats.{Later, Show}
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.scalajs.dom._
import monix.reactive.subjects.Var
import org.scalajs.dom.raw.HTMLElement
import scala.util.Try
import cats.syntax.traverse._
import cats.instances.list._
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.show._

object DOM {
  // ==Input==
  def string(name: String, default: String): Owlet[String] = {
    val signal = Var(default)
    val input = createInput(
      name,
      "text",
      default,
      e => signal := e.target.asInstanceOf[html.Input].value
    )
    Owlet(input.map(List(_)), signal)
  }

  def number(name: String, default: Double): Owlet[Double] = {
    val signal = Var(default)
    val node = createInput(name, "number", default, e => {
      val value = e.target.asInstanceOf[html.Input].value
      Try(value.toDouble).foreach(signal := _)
    }).map { input =>
      input.step = "any"
      input
    }

    Owlet(node.map(List(_)), signal)
  }

  def numberSlider(
      name: String,
      min: Double,
      max: Double,
      default: Double
  ): Owlet[Double] = {
    val signal = Var(default)
    val node = createInput(name, "range", default, e => {
      val value = e.target.asInstanceOf[html.Input].value
      Try(value.toDouble.toInt).foreach(signal := _)
    }).map { input =>
      input.step = "any"
      input.min = min.toString
      input.max = max.toString
      input
    }

    Owlet(node.map(List(_)), signal)
  }

  def int(name: String, default: Int): Owlet[Int] = {
    val signal = Var(default)
    val node = createInput(name, "number", default, e => {
      val value = e.target.asInstanceOf[html.Input].value
      Try(value.toDouble.toInt).foreach(signal := _)
    })
    Owlet(node.map(List(_)), signal)
  }

  def boolean(name: String, default: Boolean): Owlet[Boolean] = {
    val signal = Var(default)
    val node = Later {
      val input = document.createElement("input").asInstanceOf[html.Input]
      input.`type` = "checkbox"
      input.name = name
      input.className = "owlet-input-" + normalize(name)
      input.checked = default
      input.onchange = e => signal := e.target.asInstanceOf[html.Input].checked
      input
    }
    Owlet(node.map(List(_)), signal)
  }

  def intSlider(
      name: String,
      min: Int,
      max: Int,
      default: Int
  ): Owlet[Int] = {
    val signal = Var(default)
    val node = createInput(name, "range", default, e => {
      val value = e.target.asInstanceOf[html.Input].value
      Try(value.toDouble.toInt).foreach(signal := _)
    }).map { input =>
      input.step = "1"
      input.min = min.toString
      input.max = max.toString
      input
    }
    Owlet(node.map(List(_)), signal)
  }

  private def createInput[A](
      n: String,
      t: String,
      default: A,
      transform: Event => Unit,
  ) = Later {
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
    val signal = Var(default)
    el.onchange = e => signal := e.target.asInstanceOf[html.Select].value
    Owlet(Later(List(el)), signal)
  }

  /**
    * button emit `default` value immediatly and emit `pressed` value every time it's clicked
    */
  def button[A](name: String, default: A, pressed: A) = {
    val signal = Var(default)
    val node = Later {
      val el = document.createElement("button").asInstanceOf[html.Button]
      el.appendChild(document.createTextNode(name))
      el.onmousedown = _ => signal := pressed
      el.onmouseup = _ => signal := default
      el
    }
    Owlet(node.map(List(_)), signal)
  }

  /**
    * Container
    *
    * wrap nodes in `Owlet` into container element `div`, `label` etc
    * style of div can reactive from a stream of `className`
    */
  def createContainer[A, Tag <: HTMLElement](
      tag: String,
      inner: Owlet[A],
      className: Observable[Seq[String]] = Observable.empty,
      id: Option[String] = None
  ): Owlet[A] = {
    val wrapped = inner.nodes.map { nodes =>
      val el = document.createElement(tag).asInstanceOf[Tag]
      id.map(el.id = _)
      className.foreach(c => el.className = c.mkString(" "))
      nodes.foreach(el.appendChild)
      List(el)
    }
    Owlet(wrapped, inner.signal)
  }

  def div[A](
      inner: Owlet[A],
      className: Observable[Seq[String]] = Observable.empty,
      id: Option[String] = None
  ) = {
    createContainer[A, html.Div]("div", inner, className, id)
  }

  def ul[A](
      inner: Owlet[A],
      className: Observable[Seq[String]] = Observable.empty,
      id: Option[String] = None
  ) = {
    createContainer[A, html.UList]("ul", inner, className, id)
  }

  def li[A](
      inner: Owlet[A],
      className: Observable[Seq[String]] = Observable.empty,
      id: Option[String] = None
  ) = {
    createContainer[A, html.LI]("li", inner, className, id)
  }

  def label[A](inner: Owlet[A], name: String): Owlet[A] = {
    Owlet(
      inner.nodes.map { nodes =>
        val el = document.createElement("label").asInstanceOf[html.Label]
        el.appendChild(document.createTextNode(name))
        nodes.foreach(el.appendChild)
        List(el)
      },
      inner.signal
    )
  }

  def text(content: String): Owlet[Nothing] = {
    Owlet(Later(List(document.createTextNode(content))), Observable.empty)
  }

  /** Spreadsheet like fx
    * create a new Owlet with existing Owlets with a formula
    */
  def fx[A, B: Show](formula: List[A] => B, input: List[Owlet[A]]): Owlet[B] = {
    input.sequence.map(formula).flatMap(output => text(output.show))
  }

  def output[A: Show](
      input: Owlet[A],
      classNames: Observable[Seq[String]] = Var(Nil)
  ) = {
    div(input.flatMap(o => text(o.show)), classNames)
  }

  /**
    * Render
    */
  def render[A](owlet: Owlet[A], selector: String) =
    for {
      _ <- Task {
        owlet.nodes.value
          .foreach(document.querySelector(selector).appendChild)
      }
      _ <- Task(owlet.signal.subscribe)
    } yield ()

  def renderOutput[A: Show](owlet: Owlet[A], selector: String) = Task(
    render(output(owlet), selector)
  )

  private def normalize(s: String) = s.replaceAll(" ", "-").toLowerCase
}
