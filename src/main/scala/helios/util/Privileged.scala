package helios.util

import org.mavlink.messages.{IMAVLinkMessageID, MAV_CMD, MAVLinkMessage}
import org.mavlink.messages.common.msg_command_long

object Privileged {

  trait PrivilegedLike[T] {
    def isPrivileged(thing: T): Boolean
  }

  object PrivilegedLike {

    implicit object PrivilegedMAVLink extends PrivilegedLike[MAVLinkMessage] {
      lazy val privilegedMessages: Set[Int] = Set(
        IMAVLinkMessageID.MAVLINK_MSG_ID_SET_ROLL_PITCH_YAW_SPEED_THRUST,
        IMAVLinkMessageID.MAVLINK_MSG_ID_SET_ROLL_PITCH_YAW_THRUST
      )

      lazy val privilegedCommands: Set[Int] = Set(
        MAV_CMD.MAV_CMD_NAV_LAND,
        MAV_CMD.MAV_CMD_NAV_TAKEOFF
      )

      override def isPrivileged(mlmsg: MAVLinkMessage): Boolean = {
        privilegedMessages.contains(mlmsg.messageType) ||
          (mlmsg match {
            case m: msg_command_long =>
              privilegedCommands.contains(m.command)
            case _ =>
              false
          })
      }
    }

    implicit object UnprivilegedMAVLink extends PrivilegedLike[MAVLinkMessage] {
      override def isPrivileged(mlmsg: MAVLinkMessage): Boolean = false
    }

  }

}
