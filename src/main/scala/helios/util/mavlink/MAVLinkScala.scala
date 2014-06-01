package helios.util.mavlink

import org.mavlink.messages.MAVLinkMessage
import org.mavlink.messages.common._
import scala.util.{Failure, Success, Try}

@deprecated
object MAVLinkScala {

  implicit class MsgToScala(val msg: MAVLinkMessage) {
    def toScala: Try[MAVLinkCaseClass] = {
      msg.messageType match {
        case 0 =>
          Try(msg.asInstanceOf[msg_heartbeat]) map (converted =>
            HEARTBEAT(
              converted.`type`,
              converted.autopilot,
              converted.base_mode,
              converted.custom_mode,
              converted.system_status,
              converted.mavlink_version)
            )

        case 1 =>
          Try(msg.asInstanceOf[msg_sys_status]).map(converted =>
            SYS_STATUS(
              converted.onboard_control_sensors_present,
              converted.onboard_control_sensors_enabled,
              converted.onboard_control_sensors_health,
              converted.load,
              converted.voltage_battery,
              converted.current_battery,
              converted.battery_remaining,
              converted.drop_rate_comm,
              converted.errors_comm,
              converted.errors_count1,
              converted.errors_count2,
              converted.errors_count3,
              converted.errors_count4)
          )

        case _ => Failure(new Exception(s"Unknown MAVLink message, ID: ${msg.messageType}"))
      }
    }
  }

  trait MAVLinkCaseClass

  case class HEARTBEAT(_type: Int,
                       autopilot: Int,
                       base_mode: Int,
                       custom_mode: Long,
                       system_status: Int,
                       mavlink_version: Int)
    extends MAVLinkCaseClass

  case class SYS_STATUS(onboard_control_sensors_present: Long,
                        onboard_control_sensors_enabled: Long,
                        onboard_control_sensors_health: Long,
                        load: Int,
                        voltage_battery: Int,
                        current_battery: Int,
                        battery_remaining: Int,
                        drop_rate_comm: Int,
                        errors_comm: Int,
                        errors_count1: Int,
                        errors_count2: Int,
                        errors_count3: Int,
                        errors_count4: Int)
    extends MAVLinkCaseClass

}
