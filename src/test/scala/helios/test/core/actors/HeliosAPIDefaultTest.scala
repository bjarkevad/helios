package helios.test.core.actors

import org.scalatest._
import helios.api.HeliosAPI.SystemStatus
import org.mavlink.messages.{MAV_MODE, MAV_MODE_FLAG}
import helios.core.HeliosAPIDefault

class HeliosAPIDefaultTest extends FlatSpec with Matchers {

  import HeliosAPIDefault._

  "HeliosAPIDefaultTest" should "create basemode correctly" in {
    addFlag(None, 1, 2, 4, 8, 8) should be(15)
    addFlag(Some(SystemStatus(0, 0, 15, 0, 0))) should be(15)
    addFlag(Some(SystemStatus(0, 0, 15, 0, 0)), 4) should be(15)
    addFlag(Some(SystemStatus(0, 0, 15, 0, 0)), 16) should be(31)
  }

  it should "get mode" in {
    getMode(None) should be(0)
    getMode2(None) should be(0)
    getMode(None) should be(getMode2(None))

    val m = 128
    val s = Some(SystemStatus(0, 0, m, 0, 0))

    getMode(s) should be(m)
    getMode2(s) should be(m)
    getMode(s) should be(getMode2(s))
  }

  it should "add flags" in {
    val m = MAV_MODE_FLAG.MAV_MODE_FLAG_TEST_ENABLED
    val s = Some(SystemStatus(0, 0, MAV_MODE.MAV_MODE_STABILIZE_ARMED, 0))

    addFlag(s) should be(MAV_MODE.MAV_MODE_STABILIZE_ARMED)
    addFlag(s, m) should be(MAV_MODE.MAV_MODE_STABILIZE_ARMED | m)
  }

  it should "remove flags" in {
    val m = MAV_MODE_FLAG.MAV_MODE_FLAG_TEST_ENABLED
    val s = Some(SystemStatus(0, 0, MAV_MODE.MAV_MODE_STABILIZE_ARMED, 0))

    removeFlag(
      Some(
        SystemStatus(0, 0, addFlag(s, m), 0)
      ), MAV_MODE_FLAG.MAV_MODE_FLAG_TEST_ENABLED) should be(MAV_MODE.MAV_MODE_STABILIZE_ARMED)

    removeFlag(s, m) should be(MAV_MODE.MAV_MODE_STABILIZE_ARMED)
  }

  it should "determine if status has flags" in {
    val s = Some(SystemStatus(0, 0, MAV_MODE.MAV_MODE_STABILIZE_ARMED, 0))

    val trueFlags = Seq(
      MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED,
      MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
      MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
    )

    val falseFlags = Seq(
      MAV_MODE_FLAG.MAV_MODE_FLAG_HIL_ENABLED,
      MAV_MODE_FLAG.MAV_MODE_FLAG_AUTO_ENABLED,
      MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
      MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED,
      MAV_MODE_FLAG.MAV_MODE_FLAG_TEST_ENABLED
    )

    trueFlags forall (hasFlags(s, _)) should be (true)
    falseFlags forall (!hasFlags(s, _)) should be(true)
  }
}