package helios.test.api

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import helios.api.Streams._
import helios.api.HeliosAPI.{AttitudeRad, AttitudeDeg}
import scala.concurrent.Future
import org.mavlink.messages._

class StreamsAPITest extends APITestBase {
//"HeliosAPI Streams"
   ignore should "update system status" in helios.map {
    h =>
      val expectedCount = 10

      Future {
        while (true)
          setStatus(preflight)
      }

      h.systemStatusStream.take(expectedCount).toBlockingObservable
        .foreach {
        hb =>
          hb.mavtype should be(MAV_TYPE.MAV_TYPE_QUADROTOR)
          hb.autopilot should be(MAV_AUTOPILOT.MAV_AUTOPILOT_GENERIC)
          hb.mode should be(MAV_MODE.MAV_MODE_PREFLIGHT)
          hb.status should be(MAV_STATE.MAV_STATE_STANDBY)
      }
  }

  it should "update location" in helios.map {
    h =>
  }

  ignore should "update attitude in radians" in helios.map {
    h =>
      val r = 90
      val p = 180
      val y = 270
      val expectedCount = 10
      val attDeg = AttitudeDeg(r, p, y)

      Future {
        while (true)
          setAttitude(attDeg)
      }

      h.attitudeRadStream.take(expectedCount).toBlockingObservable
        .foreach {
        _ should be(AttitudeRad(
          Math.toRadians(r).toFloat,
          Math.toRadians(p).toFloat,
          Math.toRadians(y).toFloat)
        )
      }
  }

  ignore should "update attitude in degrees" in helios.map {
    h =>
      val r = 90
      val p = 180
      val y = 270
      val expectedCount = 10
      val attDeg = AttitudeDeg(r, p, y)


      Future {
        while (true)
          setAttitude(attDeg)
      }

      h.attitudeDegStream.take(expectedCount).toBlockingObservable
        .foreach(_ should be(attDeg))
  }
}
