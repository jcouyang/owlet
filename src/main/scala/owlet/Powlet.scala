package us.oyanglul.owlet

import cats._
import org.scalajs.dom._
import monix.reactive.Observable
import cats.syntax.monoid._
import cats.instances.list._

private[owlet] case class Powlet[+A](
    nodes: Eval[List[Node]],
    signal: Observable[A]
)

private[owlet] object Powlet {
  implicit val applicativePowlet = new Applicative[Powlet] {
    override def map[A, B](fa: Powlet[A])(f: A => B) = {
      Powlet(fa.nodes, fa.signal.map(f))
    }
    def ap[A, B](ff: Powlet[A => B])(fa: Powlet[A]): Powlet[B] = {
      Powlet(
        ff.nodes |+| fa.nodes,
        Observable.combineLatestMap2(ff.signal, fa.signal)(_(_))
      )
    }

    def pure[A](a: A) = Powlet(Owlet.emptyNode, Observable.pure[A](a))
  }
}
