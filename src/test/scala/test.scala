import org.scalatest._

class test extends FlatSpec with Matchers {
  "An integer" should "add correctly" in {
    1 + 1 should be (2)
  }
}