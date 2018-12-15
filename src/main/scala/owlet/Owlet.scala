package us.oyanglul.owlet

import cats._
import cats.syntax.monoid._
import org.scalajs.dom._
import monix.reactive.Observable

trait Cell[A] extends Product with Serializable {
  def fold[S](seed: => S)(op: (S, A) => S): Cell[S]
  def filter(b: A => Boolean): Cell[A]
}

case class Owlet[A](nodes: List[Node], signal: Observable[A]) extends Cell[A] {
  def fold[S](seed: => S)(op: (S, A) => S) = {
    Owlet(nodes, signal.scan(seed)(op))
  }
  def filter(b: A => Boolean) = {
    Owlet(nodes, signal.filter(b))
  }
}

trait ParallelInstances {
  implicit val parallelForOwlet: Parallel[Owlet, Powlet] =
    new Parallel[Owlet, Powlet] {
      def applicative: Applicative[Powlet] = Powlet.applicativePowlet
      def monad: Monad[Owlet] = Owlet.monadOwlet
      def sequential = Lambda[Powlet ~> Owlet](x => Owlet(x.nodes, x.signal))
      def parallel = Lambda[Owlet ~> Powlet](x => Powlet(x.nodes, x.signal))
    }
}

object Owlet extends ParallelInstances {
  implicit val monadOwlet = new Monad[Owlet] {
    override def map[A, B](fa: Owlet[A])(f: A => B) = {
      Owlet(fa.nodes, fa.signal.map(f))
    }
    def flatMap[A, B](fa: Owlet[A])(f: A => Owlet[B]): Owlet[B] = {
      flat(map(fa)(f))
    }
    def tailRecM[A, B](a: A)(f: A => Owlet[Either[A, B]]): Owlet[B] =
      f(a) match {
        case Owlet(node, signal) =>
          Owlet(node, signal.mergeMap {
            case Left(next) => Observable.tailRecM(next)(c => f(c).signal)
            case Right(b)   => Observable.pure(b)
          })
      }
    def pure[A](a: A) = Owlet(Nil, Observable.pure[A](a))

  }

  private def flat[A](item: Owlet[Owlet[A]]) = {
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

  implicit val monoidKOwlet = new MonoidK[Owlet] {
    def empty[A]: Owlet[A] = Owlet(List[Node](), Observable.empty)
    def combineK[A](x: Owlet[A], y: Owlet[A]): Owlet[A] =
      Owlet(x.nodes ++ y.nodes, Observable.from(List(x.signal, y.signal)).merge)
  }

  implicit def monoidOwlet[A: Monoid] = new Monoid[Owlet[A]] {
    def combine(a: Owlet[A], b: Owlet[A]): Owlet[A] =
      Owlet(
        a.nodes ++ b.nodes,
        Observable.combineLatestMap2(a.signal, b.signal)(_ |+| _)
      )
    def empty = Owlet(List[Node](), Observable.empty)
  }
}
