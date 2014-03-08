package helios.test.util.gpio

import org.scalatest._

import helios.util.gpio.{GPIOPin}
import helios.util.gpio.GPIO._
import scala.util.Success

@Ignore
class GPIOTest extends FlatSpec with Matchers {
  "GPIO" should "export, verify and unexport correctly" in {
    val gpio = export(GPIOPin.P8_14)

   gpio.isSuccess should be(true)

    gpio map {
      g =>
        isExported(g) should be(true)
        unexport(g).isSuccess should be(true)
        isExported(g) should be(false)
    }
  }

  it should "set and get direction correctly" in {
    val gpio = export(GPIOPin.P8_15)

    gpio.isSuccess should be(true)

    gpio map {
      g =>
        setDirection(g, true)
        getDirection(g) match {
          case Some("in") => assert(true)
          case _ => assert(false)
        }
        unexport(g).isSuccess should be(true)
    }
  }

  it should "set and get value correctly" in {
    val gpio = export(GPIOPin.P8_16)

    gpio.isSuccess should be(true)

    gpio map {
      g =>
        setValue(g, 1)
        getValue(g) match {
          case Success("1") => assert(true)
          case _ => assert(false)
        }

        setValue(g, 0)
        getValue(g) match {
          case Success("0") => assert(true)
          case _ => assert(false)
        }
    }

  }
}