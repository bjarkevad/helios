package helios

import org.scalatest.{Matchers, FlatSpec}
import org.mavlink.messages
import org.mavlink.messages.common.msg_heartbeat
import org.mavlink.messages._
import org.mavlink.MAVLinkReader
import helios.mavlink.MAVLinkScala.MsgToScala

/**
 * Created by bjarke on 2/5/14.
 */
class mavlinktest extends FlatSpec with Matchers {

  "mavlink" should "work" in {

  }

  it should "create a heartbeat package properly" in {
    val hb = new msg_heartbeat(20, MAV_COMPONENT.MAV_COMP_ID_IMU)
    hb.sequence = 0
    hb.`type` = MAV_TYPE.MAV_TYPE_FIXED_WING
    hb.autopilot = MAV_AUTOPILOT.MAV_AUTOPILOT_GENERIC
    hb.base_mode = MAV_MODE_FLAG.MAV_MODE_FLAG_AUTO_ENABLED //MAV_MODE.MAV_MODE_PREFLIGHT
    hb.custom_mode = 0
    hb.system_status = MAV_STATE.MAV_STATE_STANDBY
    hb.mavlink_version = 3

    val msg = hb.encode()

    msg should be(Array[Byte](-2,9,0,20,-56,0,0,0,0,0,1,0,4,3,3,88,-60))
  }

  it should "interpret a heartbeat package properly" in {
    val msg = Array[Byte](-2,9,0,20,-56,0,0,0,0,0,1,0,4,3,3,88,-60)

    val reader = new MAVLinkReader()
    val convMsg = reader.getNextMessage(msg, msg.length)
    convMsg.isInstanceOf[msg_heartbeat] should be(true)
    val m = convMsg.asInstanceOf[msg_heartbeat]
    m.sequence should be(0)
    m.`type` should be(MAV_TYPE.MAV_TYPE_FIXED_WING)
    m.autopilot should be(MAV_AUTOPILOT.MAV_AUTOPILOT_GENERIC)
    m.base_mode should be(MAV_MODE_FLAG.MAV_MODE_FLAG_AUTO_ENABLED)
    m.custom_mode should be(0)
    m.system_status should be(MAV_STATE.MAV_STATE_STANDBY)
    m.mavlink_version should be(3)
  }

  implicit class Converter(val a: Array[Byte]) {
    def byteArrayToHex: String = {
      BigInt(1, a).toString(16)
    }

    def byteArrayToBinary: String = {
      BigInt(1, a).toString(2)
    }
  }

}


