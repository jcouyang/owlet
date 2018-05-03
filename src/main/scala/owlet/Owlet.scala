package owlet

import monix.reactive.Observable
import outwatch.dom._
import outwatch.dom.dsl._
import scala.concurrent.duration._
import monix.execution.Scheduler.Implicits.global
object Owlet {
  def main(args: scala.Array[String]): Unit = {
    val seconds: Observable[Int] = Observable.interval(1.second)
      .map(_+1).map(_.toInt)

    val root = div("Seconds elapsed: ", child <-- seconds)
    OutWatch.renderInto("#app", root) unsafeRunSync ()
  }
}
