package helios.util

import org.mavlink.messages.{IMAVLinkMessageID, MAV_CMD, MAVLinkMessage}
import org.mavlink.messages.common.msg_command_long

object Privileged {

  /**
   * defines that T can act as a PrivilegedLike
   * @tparam T the type of the thing that needs the isPrivileged function
   */
  trait PrivilegedLike[T] {
    def isPrivileged(thing: T): Boolean
  }

  /**
   * Contains concrete PrivilegedLike's
   */
  object PrivilegedLike {

    /**
     * Defines which MAVLink messages should be considered privileged,
     * as well as implementing the isPrivileged function
     */
    implicit object PrivilegedMAVLink extends PrivilegedLike[MAVLinkMessage] {
      lazy val privilegedMessages: Set[Int] = Set(
        IMAVLinkMessageID.MAVLINK_MSG_ID_SET_ROLL_PITCH_YAW_SPEED_THRUST,
        IMAVLinkMessageID.MAVLINK_MSG_ID_SET_ROLL_PITCH_YAW_THRUST
      )

      lazy val privilegedCommands: Set[Int] = Set(
        MAV_CMD.MAV_CMD_NAV_LAND,
        MAV_CMD.MAV_CMD_NAV_TAKEOFF
      )

      /**
       * determines if a MAVLink message is privileged or not
       * @param mlmsg the message which should be checked for privileges
       * @return true if privileged, false if not
       */
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

    /**
     * Defines that no MAVLink messages should not be considered privileged
     */
    implicit object UnprivilegedMAVLink extends PrivilegedLike[MAVLinkMessage] {
      override def isPrivileged(mlmsg: MAVLinkMessage): Boolean = false
    }

  }

}
