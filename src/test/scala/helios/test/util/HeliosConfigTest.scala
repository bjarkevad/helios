package helios.test.util

import org.scalatest.{FlatSpec, Matchers}
import helios.util.HeliosConfig
import akka.actor.ActorSystem

class HeliosConfigTest extends FlatSpec with Matchers {

  "HeliosConfig" should "load the correct file" in {

  }

  it should "fall back to default values" in {

  }

  it should "fail if config file is invalid" in {

  }

  it should "load serialports" in {
    val hc = HeliosConfig("./src/test/resources/helios.conf")
    val sp = hc.serialports

    sp shouldNot be(List.empty)
  }
}
