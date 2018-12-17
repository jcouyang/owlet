package us.oyanglul.owlet

import cats._
import org.scalajs.dom._
import monix.reactive.Observable

private[owlet] case class Powlet[+A](nodes: List[Node], signal: Observable[A])
    extends Cell[A] {
  def fold[S](seed: => S)(op: (S, A) => S) = {
    Powlet(nodes, signal.scan(seed)(op))
  }
  def filter(b: A => Boolean) = {
    Powlet(nodes, signal.filter(b))
  }
}

private[owlet] object Powlet {
  implicit val applicativePowlet = new Applicative[Powlet] {
    override def map[A, B](fa: Powlet[A])(f: A => B) = {
      Powlet(fa.nodes, fa.signal.map(f))
    }
    def ap[A, B](ff: Powlet[A => B])(fa: Powlet[A]): Powlet[B] = {
      Powlet(
        ff.nodes ++ fa.nodes,
        Observable.combineLatestMap2(ff.signal, fa.signal)(_(_))
      )
    }

    def pure[A](a: A) = Powlet(Nil, Observable.pure[A](a))
  }
}
