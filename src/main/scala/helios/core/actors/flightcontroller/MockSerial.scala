package helios.core.actors.flightcontroller

import akka.actor.{Props, Actor}
import org.slf4j.LoggerFactory
import com.github.jodersky.flow.Serial
import org.mavlink.messages.common._
import org.mavlink.messages._
import helios.mavlink.MAVLink.convertToMAVLink
import helios.core.actors.flightcontroller.FlightControllerMessages.WriteMAVLink
import akka.util.ByteString
import helios.api.messages.MAVLinkMessages.PublishMAVLink

object MockSerial {
  def props = Props(new MockSerial)
}


//TODO: Does not respond to ack requests
class MockSerial extends Actor {
  import scala.concurrent.duration._
  import scala.language.postfixOps
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val logger = LoggerFactory.getLogger(classOf[MockSerial])

  var s = 0
  def heartbeat: MAVLinkMessage = {
    val hb = new msg_heartbeat(20, MAV_COMPONENT.MAV_COMP_ID_IMU)
    hb.sequence = s
    s += 1
    hb.`type` = MAV_TYPE.MAV_TYPE_QUADROTOR
    hb.autopilot = MAV_AUTOPILOT.MAV_AUTOPILOT_GENERIC
    hb.base_mode = MAV_MODE.MAV_MODE_STABILIZE_ARMED
    hb.custom_mode = 0
    hb.system_status = MAV_STATE.MAV_STATE_STANDBY
    hb.mavlink_version = 3

    hb
  }

  lazy val sysStatus: MAVLinkMessage = {
    val st = new msg_sys_status(20, 1)
    st.battery_remaining = 75
    st.current_battery = 3000
    st.onboard_control_sensors_health = 256
    st.onboard_control_sensors_enabled = 256
    st.onboard_control_sensors_present = 256
    st.load = 500
    st.drop_rate_comm = 10

    st
  }
  override def preStart() = {
    context.system.scheduler.schedule(0 millis, 1 second, context.parent, PublishMAVLink(heartbeat))
    context.system.scheduler.schedule(250 millis, 500 millis, context.parent, PublishMAVLink(sysStatus))
  }

  override def receive: Actor.Receive = {
    case Serial.Open(s) => sender ! Serial.Opened(s, self)

    case WriteMAVLink(ml) if ml.isInstanceOf[msg_set_roll_pitch_yaw_thrust] =>
      logger.debug("set rpyt")

    case m@WriteMAVLink(ml) =>
      logger.debug(m.toString)

    case Serial.Write(bs, _) =>
      convertToMAVLink(bs) map {
        case mll: msg_set_roll_pitch_yaw_thrust =>
//          val msg = new msg_attitude_quaternion(20, 1)
//          msg.q1 = 0.5f
//          msg.q2 = 0.5f
//          msg.q3 = 0.5f
//          msg.q4 = 0.5f
//          msg.rollspeed = 1
//          msg.pitchspeed = 1
//          msg.yawspeed = 1
//          msg.time_boot_ms = 1
          val msg = new msg_attitude(20, 1)
          msg.pitch = mll.pitch
          msg.roll = mll.roll
          msg.yaw = mll.yaw
          sender ! Serial.Received(ByteString(msg.encode()))
          //context.parent ! PublishMAVLink(msg)
        case _ =>
      }
    case m@_ => logger.debug(s"Received $m")
  }
}
