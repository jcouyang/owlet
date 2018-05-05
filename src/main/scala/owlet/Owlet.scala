package owlet

import cats.Functor
import cats.data.Reader
import outwatch.dom.{  _ }
import outwatch.dom.dsl._
import monix.execution.Scheduler.Implicits.global
import outwatch.Handler._
import cats.syntax.functor._
import cats.{~>, Id}
object Main {

  case class Owlet[A](node:Id ~> ({type RR[C] = Reader[Observable[C], VNode]})#RR, signal: Observable[A])

  implicit val functorOwlet = new Functor[Owlet] {
    def map[A,B](fa: Owlet[A])(f: A=>B) = {
      Owlet(fa.node, fa.signal.map(f))
    }
  }

  def oinput(n: String) = create[String].map(handler =>
    Owlet(new (Id ~> ({type RR[C] = Reader[Observable[C], VNode]})#RR) {
      def apply[A](a:A):Reader[Observable[A], VNode] = Reader((signal:Observable[A]) => input(tpe := "text", name := n, onInput.value --> handler))
    }, handler)
    )

  def main(args: scala.Array[String]): Unit = {
    // val output = oinput("hehe").map(o => o.map(_ + a))
    // OutWatch.renderInto("#app", output.flatMap(_.node)) unsafeRunSync ()
  }
}
