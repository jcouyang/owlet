package us.oyanglul.owlet

import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.scalajs.dom._
import monix.reactive.subjects.Var
import scala.util.Try
import cats.syntax.traverse._
import cats.instances.list._

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
    Owlet(List(input), state)
  }

  def number(name: String, default: Double): Owlet[Double] = {
    val state = Var(default)
    val input = createInput(name, "number", default, e => {
      val value = e.target.asInstanceOf[html.Input].value
      Try(value.toDouble).foreach(state := _)
    })
    input.step = "any"
    Owlet(List(input), state)
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
    Owlet(List(input), state)
  }

  def int(name: String, default: Int): Owlet[Int] = {
    val state = Var(default)
    val input = createInput(name, "number", default, e => {
      val value = e.target.asInstanceOf[html.Input].value
      Try(value.toDouble.toInt).foreach(state := _)
    })
    Owlet(List(input), state)
  }

  def boolean(name: String, default: Boolean): Owlet[Boolean] = {
    val sink = Var(default)
    val input = document.createElement("input").asInstanceOf[html.Input]
    input.`type` = "checkbox"
    input.name = name
    input.className = "owlet-input-" + normalize(name)
    input.checked = default
    input.onchange = e => sink := e.target.asInstanceOf[html.Input].checked
    Owlet(List(input), sink)
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
    Owlet(List(input), state)
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
    Owlet(List(el), sink)
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
    Owlet(List(el), sink)
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
    inner.nodes.foreach(el.appendChild)
    Owlet(List(el), inner.signal)
  }

  def label[A](inner: Owlet[A], name: String): Owlet[A] = {
    val el = document.createElement("label").asInstanceOf[html.Label]
    el.appendChild(document.createTextNode(name))
    inner.nodes.foreach(el.appendChild)
    Owlet(List(el), inner.signal)
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
      owlets.foreach { owlet =>
        val li = document.createElement("li").asInstanceOf[html.LI]
        owlet.nodes.foreach(li.appendChild)
        ul.appendChild(li)
      }
      owlets.sequence.signal.foreach(sink := _)
    }
    Owlet(List(ul), sink)
  }

  def flat[A](item: Owlet[Owlet[A]]) = {
    val div: html.Div = document.createElement("div").asInstanceOf[html.Div]
    Owlet(
      List(div),
      item.signal
        .flatMapLatest { owlet =>
          while (div.lastChild != null) {
            div.removeChild(div.lastChild)
          }
          owlet.nodes.foreach(div.appendChild)
          owlet.signal
        }
    )
  }

  def removableList[A](
      items: Observable[List[Owlet[A]]],
      actions: Var[List[Owlet[A]] => List[Owlet[A]]]
  ): Owlet[List[A]] = {
    val sink = Var(List[A]())
    val ul: html.UList = document.createElement("ul").asInstanceOf[html.UList]
    items.foreach { owlets =>
      while (ul.lastChild != null) {
        ul.removeChild(ul.lastChild)
      }
      owlets.foreach { owlet =>
        val li = document.createElement("li").asInstanceOf[html.LI]
        owlet.nodes.foreach { node =>
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
        ul.appendChild(li)
      }
      owlets.sequence.signal.foreach(sink := _)
    }
    Owlet(List(ul), sink)
  }

  /** Spreadsheet like fx
    * create a new Owlet with existing Owlets with a formula
    */
  def fx[A, B](formula: List[A] => B, input: List[Owlet[A]]): Owlet[B] = {
    val div: html.Div = document.createElement("div").asInstanceOf[html.Div]
    val sink = input.sequence.signal.map(formula)
    sink.foreach(a => div.textContent = a.toString)
    Owlet(List(div), sink)
  }

  def output[A](
      input: Owlet[A],
      classNames: Observable[Seq[String]] = Var(Nil)
  ) = {
    val div = document.createElement("div").asInstanceOf[html.Div]
    classNames.foreach(c => div.className = c.mkString(" "))
    div.className += " owlet-output"
    input.signal.foreach(v => div.innerHTML = v.toString)
    input.nodes :+ div
  }

  /**
    * Render
    */
  def render[A](owlet: Owlet[A], selector: String) = {
    owlet.nodes
      .foreach(document.querySelector(selector).appendChild)
    owlet.signal.subscribe
  }

  def renderOutput[A](owlet: Owlet[A], selector: String) = {
    output(owlet)
      .foreach(document.querySelector(selector).appendChild)
  }

  private def normalize(s: String) = s.replaceAll(" ", "-").toLowerCase
}
