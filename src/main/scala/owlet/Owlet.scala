package us.oyanglul

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
  // Input
  def string(default: String):Owlet[String] = {
    string("", default)
  }

  def string(n: String, default: String): Owlet[String] = {
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
    el.onmousedown = _ => sink := pressed
    el.onmouseup = _ => sink := default
    Owlet(List(el), sink)
  }

  def ul[A](items: Observable[List[A]], createEl: A => Owlet[A]) = {
    val sink = Var(List[A]())
    val ul: html.UList = document.createElement("ul").asInstanceOf[html.UList]
    items.foreach{item=>
      while(ul.lastChild != null) {
        ul.removeChild(ul.lastChild)
      }
      val owlets = item.map{i =>
        val li = document.createElement("li").asInstanceOf[html.LI]
        val owlet = createEl(i)
        owlet.nodes.foreach(li.appendChild(_))
        ul.appendChild(li)
        owlet
      }
      owlets.sequence.signal.map(sink := _)
    }
    Owlet(List(ul), sink)
  }

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
      owlets.sequence.signal.map(sink:=_)
    }
    Owlet(List(ul), sink)
  }

  def fx[A,B] (formula:List[A] => B, input:List[Owlet[A]]): Owlet[B] = {
    val div:html.Div = document.createElement("div").asInstanceOf[html.Div]
    val sink = input.sequence.signal.map(formula)
    sink.foreach(a=>div.textContent = a.toString)
    Owlet(List(div), sink)
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

  def render[A](owlet: Owlet[A], selector: String) = {
    owlet.nodes
      .foreach(document.querySelector(selector).appendChild(_))
  }

  def renderAppend[A](owlet: Owlet[A], selector: String) = {
    output(owlet)
      .foreach(document.querySelector(selector).appendChild(_))
  }
}
