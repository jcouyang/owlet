package us.oyanglul.owlet

import cats._
import cats.syntax.monoid._
import org.scalajs.dom._
import monix.reactive.Observable
import cats.instances.list._

case class Owlet[+A](nodes: Eval[List[Node]], signal: Observable[A]) {
  def fold[S](seed: => S)(op: (S, A) => S) = {
    Owlet(nodes, signal.scan(seed)(op))
  }
}

trait ParallelInstances {
  implicit val parallelForOwlet: Parallel[Owlet, Par] =
    new Parallel[Owlet, Par] {
      def applicative: Applicative[Par] = Par.applicativePowlet
      def monad: Monad[Owlet] = Owlet.monadOwlet
      def sequential = Lambda[Par ~> Owlet](x => Owlet(x.nodes, x.signal))
      def parallel = Lambda[Owlet ~> Par](x => Par(x.nodes, x.signal))
    }
}

object $ {
  import org.scalajs.dom._
  import monocle._
  import monocle.macros.GenPrism

  val nodes =
    Lens[Owlet[_], List[Node]](_.nodes.value)(
      n => a => Owlet(Later(n), a.signal)
    )
  val eachNode = nodes composeTraversal Traversal.fromTraverse[List, Node]
  val input = eachNode composePrism GenPrism[Node, html.Input]
  val div = eachNode composePrism GenPrism[Node, html.Div]
}

object Owlet extends ParallelInstances {
  val emptyNode = Later(List[Node]())

  implicit val functorOwlet = new Functor[Owlet] {
    override def map[A, B](fa: Owlet[A])(f: A => B) = {
      Owlet(fa.nodes, fa.signal.map(f))
    }
  }

  implicit val functorFilterOwlet = new FunctorFilter[Owlet] {
    override def functor = functorOwlet
    override def mapFilter[A, B](fa: Owlet[A])(f: A => Option[B]): Owlet[B] = {
      Owlet(
        fa.nodes,
        fa.signal
          .flatMap(f(_) match {
            case Some(a) => Observable.pure(a)
            case None    => Observable.empty
          })
      )
    }
  }

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
    def pure[A](a: A) = Owlet(emptyNode, Observable.pure[A](a))
  }

  private def flat[A](item: Owlet[Owlet[A]]) = {
    val div: html.Div = document.createElement("div").asInstanceOf[html.Div]
    Owlet(
      Later(List(div)),
      item.signal
        .flatMapLatest { owlet =>
          while (div.lastChild != null) {
            div.removeChild(div.lastChild)
          }
          owlet.nodes.value.foreach(div.appendChild)
          owlet.signal
        }
    )
  }

  implicit val monoidKOwlet = new MonoidK[Owlet] {
    def empty[A]: Owlet[A] = Owlet(emptyNode, Observable.empty)
    def combineK[A](x: Owlet[A], y: Owlet[A]): Owlet[A] =
      Owlet(
        x.nodes |+| y.nodes,
        Observable.from(List(x.signal, y.signal)).merge
      )
  }

  implicit def monoidOwlet[A: Monoid] = new Monoid[Owlet[A]] {
    def combine(a: Owlet[A], b: Owlet[A]): Owlet[A] =
      Owlet(
        a.nodes |+| b.nodes,
        Observable.combineLatestMap2(a.signal, b.signal)(_ |+| _)
      )
    def empty = monoidKOwlet.empty[A]
  }
}
