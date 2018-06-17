package us.oyanglul.owlet

import cats._
import cats.tests.CatsSuite
import cats.laws.discipline.FunctorTests
import org.scalacheck._
import Arbitrary.arbitrary
import DOM._
class OwletSpec extends CatsSuite {

  implicit def eqOwlet[A: Eq]: Eq[Owlet[A]] = Eq.fromUniversalEquals

  implicit def arbOwletInt: Arbitrary[Owlet[Int]] =
    Arbitrary((for {
      e <- arbitrary[Int]
      s <- arbitrary[String]
    } yield int(s, e)))
  checkAll("Owlet.FunctorLaws", FunctorTests[Owlet].functor[Int, Int, Int])
}
