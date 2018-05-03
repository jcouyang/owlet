
package owlet

import org.scalatest._
import org.scalajs.dom._
import outwatch.dom._

class OwletSpec extends FlatSpec with Matchers with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    val root = document.createElement("div")
    root.id = "app"
    document.body.appendChild(root)
  }

  override def afterEach(): Unit = {
    document.body.innerHTML = ""
  }

  "You" should "probably add some tests" in {

    val message = "Hello World!"
    OutWatch.render("#app", h1(message))

    document.body.innerHTML.contains(message) shouldBe true
  }
}
