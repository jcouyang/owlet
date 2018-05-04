package owlet

import outwatch.dom.{  _ }
import outwatch.dom.dsl._
import monix.execution.Scheduler.Implicits.global
import outwatch.Handler._
// import cats.syntax.apply._
object Owlet {

  case class OComponent[A](handler: Handler[A], node:VNode)

  def main(args: scala.Array[String]): Unit = {
    val source = create[String]
    val inputE = source.map(handler => OComponent(handler,input(tpe := "text", name := "hehe", onInput.value --> handler)))

    val d = inputE.flatMap(i=> div(i.node, child <-- i.handler))
    OutWatch.renderInto("#app", d) unsafeRunSync ()
  }
}
