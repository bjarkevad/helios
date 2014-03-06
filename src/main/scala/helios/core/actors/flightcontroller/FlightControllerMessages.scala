package helios.core.actors.flightcontroller

import akka.util.ByteString
import com.github.jodersky.flow.Serial.Event
import org.mavlink.messages.MAVLinkMessage

object FlightControllerMessages {

  case class WriteAck(data: ByteString) extends Event

  case class WriteData(data: String) extends Event

  case class WriteMAVLink(msg: MAVLinkMessage)

  case class WroteMAVLink(msg: MAVLinkMessage)

  case class ReceivedMAVLink(msg: MAVLinkMessage)

  def formatData(data: ByteString) = data.mkString("[", ",", "]") + " " + new String(data.toArray, "UTF-8")
}
