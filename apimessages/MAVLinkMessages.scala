package helios.apimessages

import org.mavlink.messages.MAVLinkMessage


object MAVLinkMessages {
  case class RawMAVLink(message: MAVLinkMessage) extends PutRequest
}
