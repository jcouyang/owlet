package us.oyanglul.owlet

import cats._
import org.scalajs.dom._
import monix.reactive.Observable
import cats.syntax.monoid._
import cats.instances.list._

private[owlet] case class Par[+A](
    nodes: Eval[List[Node]],
    signal: Observable[A]
)

private[owlet] object Par {
  implicit val applicativePowlet = new Applicative[Par] {
    override def map[A, B](fa: Par[A])(f: A => B) = {
      console.log("par mapping")
      Par(fa.nodes, fa.signal.map(f))
    }
    def ap[A, B](ff: Par[A => B])(fa: Par[A]): Par[B] = {
      console.log("applying.....")
      Par(
        ff.nodes |+| fa.nodes,
        Observable.combineLatestMap2(ff.signal, fa.signal) { (fff, ffa) =>
          val res = fff(ffa)
          console.log("combining....", res.toString())
          res
        }
      )
    }

    def pure[A](a: A) = Par(Owlet.emptyNode, Observable.pure[A](a))
  }
}
