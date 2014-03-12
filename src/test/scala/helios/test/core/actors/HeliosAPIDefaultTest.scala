package helios.test.core.actors

import org.scalatest._
import helios.core.actors.HeliosAPIDefault
import helios.api.HeliosAPI.SystemStatus

class HeliosAPIDefaultTest extends FlatSpec with Matchers {

  "HeliosAPIDefaultTest" should "create basemode correctly" in {
    HeliosAPIDefault.createBasemode(None, 1, 2, 4, 8, 8) should be(15)
    HeliosAPIDefault.createBasemode(Some(SystemStatus(0, 0, 15, 0, 0))) should be(15)
    HeliosAPIDefault.createBasemode(Some(SystemStatus(0, 0, 15, 0, 0)), 4) should be(15)
    HeliosAPIDefault.createBasemode(Some(SystemStatus(0, 0, 15, 0, 0)), 16) should be(31)
  }

  it should "" in {

  }
}