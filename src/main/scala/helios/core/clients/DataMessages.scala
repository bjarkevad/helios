package helios.core.clients

import akka.util.ByteString
import com.github.jodersky.flow.Serial.{Command, Event}
import org.mavlink.messages.MAVLinkMessage

/**
 * Data messages used internally in the system
 */
object DataMessages {

  /**
   * Ack used for UARTs
   * @param data the data which were written
   */
  case class WriteAck(data: ByteString) extends Event

  /**
   * Ack used for MAVLink messages
   * @param mavlinkMsg the message which was written
   */
  case class MAVLinkWriteAck(mavlinkMsg: MAVLinkMessage) extends Event

  /**
   * Messaged represents a message read form the UART
   * @param msg the message read
   */
  case class ReceivedMAVLink(msg: MAVLinkMessage) extends Event

  /**
   * Write data
   * @param data the data
   */
  case class WriteData(data: String) extends Command

  /**
   * Write MAVLink
   * @param msg the MAVLink message
   */
  case class WriteMAVLink(msg: MAVLinkMessage) extends Command

}
