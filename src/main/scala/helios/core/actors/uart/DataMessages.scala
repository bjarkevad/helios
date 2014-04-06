package helios.core.actors.uart

import akka.util.ByteString
import com.github.jodersky.flow.Serial.{Command, Event}
import org.mavlink.messages.MAVLinkMessage

object DataMessages {

  case class WriteAck(data: ByteString) extends Event

  case class MAVLinkWriteAck(mavlinkMsg: MAVLinkMessage) extends Event

  case class ReceivedMAVLink(msg: MAVLinkMessage) extends Event

  case class WriteData(data: String) extends Command

  case class WriteMAVLink(msg: MAVLinkMessage) extends Command

}
