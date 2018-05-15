package owlet

import cats.Applicative
import cats.effect.IO
import cats.{ Functor }
import outwatch.dom.{  _ }
import outwatch.dom.dsl._
import monix.execution.Scheduler.Implicits.global
import outwatch.Handler._
import cats.syntax.functor._
import cats.syntax.apply._
import monix.reactive.Observable
object Main {

  case class Owlet[A](node:VNode, signal: Observable[A])
  implicit val functorOwlet = new Functor[Owlet] {
    def map[A,B](fa: Owlet[A])(f: A=>B) = {
      Owlet(fa.node, fa.signal.map(f))
    }
  }

  implicit val applicativeOwlet = new Applicative[Owlet] {
    def ap[A, B](ff: Owlet[A => B])(fa: Owlet[A]): Owlet[B] = Owlet(fa.node, ff.signal.ap(fa.signal))
    def pure[A](a: A) = Owlet(div(), Observable.pure[A](a))
  }

  case class OwletT[F[_], A](value: F[Owlet[A]]) {
    def map[B](f:A=>B)(implicit F: Functor[F]): OwletT[F, B] = {
      OwletT(F.map(value)(_.map(f)))
    }
  }

  def string(n: String) = create[String].map(handler =>
    Owlet(input(tpe := "text", name := n, onInput.value --> handler),handler)
    )

  def int(n: String) = create[Int].map(handler =>
    Owlet(input(tpe := "number", name := n, onInput.value.map(v=>v.toDouble.toInt) --> handler), handler)
  )

  def number(n: String): IO[Owlet[Double]] = create[Double].map{ handler =>
    val element = IO(onInput.value.map(v=>v.toDouble))
      .flatMap(e => input(tpe := "number", name := n, e --> handler))
    Owlet(element, handler)
  }

  def ooutput[A](o: Owlet[A]) = div(o.node, child <-- o.signal.map(_.toString))

  def main(args: scala.Array[String]): Unit = {
    OutWatch.renderInto("#app", OwletT((number("hehe"), number("hoho")).mapN((a,b) =>a + b)).value.flatMap(ooutput)) unsafeRunSync ()
  }
}
