package helios.core.actors.flightcontroller

import akka.util.ByteString
import com.github.jodersky.flow.Serial.{Command, Event}
import org.mavlink.messages.MAVLinkMessage

object FlightControllerMessages {

  case class WriteAck(data: ByteString) extends Event
  case class MAVLinkWriteAck(mavlinkMsg: MAVLinkMessage) extends Event

  case class ReceivedMAVLink(msg: MAVLinkMessage) extends Event

  case class WriteData(data: String) extends Command
  case class WriteMAVLink(msg: MAVLinkMessage) extends Command

  def formatData(data: ByteString) = data.mkString("[", ",", "]") + " " + new String(data.toArray, "UTF-8")
}
