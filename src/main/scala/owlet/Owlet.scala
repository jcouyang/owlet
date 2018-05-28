package us.oyanglul.owlet

import cats.{ Applicative, Monoid, MonoidK }
import cats.{ Functor }
import cats.syntax.monoid._
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.scalajs.dom._
import monix.reactive.subjects.Var
import scala.util.Try
import cats.syntax.traverse._
import cats.instances.list._

case class Owlet[A](nodes:List[Node], signal: Observable[A]) {
  def fold[S](seed: =>S)(op:(S,A)=>S) = {
    Owlet(nodes, signal.scan(seed)(op))
  }
}

object Owlet {
  implicit val functorOwlet = new Functor[Owlet] {
    def map[A,B](fa: Owlet[A])(f: A=>B) = {
      Owlet(fa.nodes, fa.signal.map(f))
    }
  }

  implicit val applicativeOwlet = new Applicative[Owlet] {
    def ap[A, B](ff: Owlet[A => B])(fa: Owlet[A]): Owlet[B] = Owlet( ff.nodes ++ fa.nodes, Observable.combineLatestMap2(ff.signal,fa.signal)(_(_)))
    def pure[A](a: A) = Owlet(Nil, Observable.pure[A](a))
  }

  implicit val monoidKOwlet = new MonoidK[Owlet] {
    def empty[A]: Owlet[A] = Owlet(List[Node](), Observable.empty)
    def combineK[A](x: Owlet[A],y: Owlet[A]): Owlet[A] = Owlet(x.nodes ++ y.nodes, Observable.merge(x.signal, y.signal))
  }

  implicit def monoidOwlet[A:Monoid] = new Monoid[Owlet[A]] {
    def combine(a: Owlet[A], b:Owlet[A]):Owlet[A] = Owlet(a.nodes ++ b.nodes, Observable.combineLatestMap2(a.signal, b.signal)(_ |+| _))
    def empty = Owlet(List[Node](), Observable.empty)
  }
}

object DOM {
  // ==Input==
  def string(name: String = "_", default: String): Owlet[String] = {
    val state = Var(default)
    val input = createInput(name, "text", default, e => state := e.target.asInstanceOf[html.Input].value)
    Owlet(List(input), state)
  }

  def number(name: String = "_", default: Double):Owlet[Double] = {
    val state = Var(default)
    val input = createInput(name, "number", default, e => {
      val value = e.target.asInstanceOf[html.Input].value
      Try(value.toDouble).foreach(state := _)
    })
    input.step = "any"
    Owlet(List(input), state)
  }

  def int(name: String = "_", default: Int): Owlet[Int] = {
    val state = Var(default)
    val input = createInput(name, "number", default, e => {
      val value = e.target.asInstanceOf[html.Input].value
      Try(value.toDouble.toInt).foreach(state := _)
    })
    Owlet(List(input), state)
  }

  private def createInput[A](n: String, t: String, default: A, transform: Event=>Unit, className:String="_") = {
    val input:html.Input = document.createElement("input").asInstanceOf[html.Input]
    input.`type` = t
    input.name = n
    input.className = "owlet-input-" + className
    input.defaultValue = default.toString
    input.oninput = e => transform(e)
    input
  }

  /**
  * Select
  */
  def select(name: String = "_", source: Observable[Map[String, String]], default: String):Owlet[String] = {
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
  def button[A](name: String = "_", default: A, pressed: A) = {
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

  def div[A](inner: Owlet[A], className: Observable[String], id: String): Owlet[A] = {
    val el = document.createElement("div").asInstanceOf[html.Div]
    el.id = id
    className.foreach(c=>el.className=c.mkString(" "))
    inner.nodes.foreach(el.appendChild(_))
    Owlet(List(el), inner.signal)
  }

  def label[A](inner: Owlet[A], name: String): Owlet[A] = {
    val el = document.createElement("label").asInstanceOf[html.Label]
    el.htmlFor = "owlet-input-" + name
    inner.nodes.foreach(el.appendChild(_))
    Owlet(List(el), inner.signal)
  }

  /** ==Output==
  *
  */
  def list[A](items:Owlet[List[Owlet[A]]]) = {
    val sink = Var(List[A]())
    val ul: html.UList = document.createElement("ul").asInstanceOf[html.UList]
    items.signal.foreach{owlets=>
      while(ul.lastChild != null) {
        ul.removeChild(ul.lastChild)
      }
      owlets.foreach{owlet =>
        val li = document.createElement("li").asInstanceOf[html.LI]
        owlet.nodes.foreach(li.appendChild(_))
        ul.appendChild(li)
      }
      owlets.sequence.signal.foreach(sink:=_)
    }
    Owlet(List(ul), sink)
  }

  /** Spreadsheet like fx
  * create a new Owlet with existing Owlets with a formula
  */
  def fx[A,B] (formula:List[A] => B, input:List[Owlet[A]]): Owlet[B] = {
    val div:html.Div = document.createElement("div").asInstanceOf[html.Div]
    val sink = input.sequence.signal.map(formula)
    sink.foreach(a=>div.textContent = a.toString)
    Owlet(List(div), sink)
  }

  def output[A](input: Owlet[A], classNames: Observable[List[String]] = Var(Nil)) ={
    val div = document.createElement("div").asInstanceOf[html.Div]
    classNames.foreach(c=>div.className = c.mkString(" "))
    input.signal.foreach(v => div.innerHTML = v.toString)
    input.nodes :+ div
  }

  /**
  * Render
  */
  def render[A](owlet: Owlet[A], selector: String) = {
    owlet.nodes
      .foreach(document.querySelector(selector).appendChild(_))
  }

  def renderAppend[A](owlet: Owlet[A], selector: String) = {
    output(owlet)
      .foreach(document.querySelector(selector).appendChild(_))
  }
}
