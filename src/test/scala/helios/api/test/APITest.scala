package helios.api.test

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import java.lang.System.currentTimeMillis
import helios.core.actors.flightcontroller.FlightControllerMessages._
import helios.api.HeliosAPI.{CommandFailure, CommandSuccess}
import helios.api.Handlers._

class APITest extends APITestBase {

  "Helios API" should "connect correctly to the core" in helios.map {
    h =>
      val res = h.ping(currentTimeMillis).block
      println(s"Time: $res")
      (res >= 0) should be(true)
  }

  it should "ONLY allow calibrating sensors when not flying" in helios.map {
    h =>
      setStatus(default)
      h.calibrateSensors map (_ should be(CommandSuccess()))
      uart.expectMsgClass(classOf[WriteMAVLink])

      setStatus(flying)
      h.calibrateSensors.block.getClass should be(classOf[CommandFailure])
  }

  it should "call the critical handler when system enters critical mode" in helios.map {
    h =>
      h.setCriticalHandler(() => probe.send(probe.ref, "CRITICAL"))
      setStatus(critical)
      probe.expectMsg(150 millis, "CRITICAL")
  }

  it should "call the emergency handler when system enters emergency mode" in helios.map {
    h =>
      h.setEmergencyHandler(() => probe.send(probe.ref, "EMERGENCY"))
      setStatus(emergency)
      probe.expectMsg(150 millis, "EMERGENCY")
  }

  it should "allow arming and disarming motors in the correct modes" in helios.map {
    h =>

  }

  it should "allow landing" in helios.map {
    h =>

  }

  it should "allow takeoff" in helios.map {
    h =>

  }

  it should "allow taking control" in helios.map {
    h =>

  }

  it should "allow leaving control to the system" in helios.map {
    h =>

  }

  it should "allow change in attitude if API has control" in helios.map {
    h =>

  }

  it should "not allow change in attitude if API does not have control" in helios.map {
    h =>

  }

  it should "not allow change in attitude beyond the predefined limits" in helios.map {
    h =>

  }

  it should "" in helios.map {
    h =>

  }
}