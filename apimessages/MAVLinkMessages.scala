package helios.apimessages

import akka.util.ByteString
import org.mavlink.messages.MAVLinkMessage
import helios.apimessages.{PutRequest}


object MAVLinkMessages {
  case class RawMAVLink(message: MAVLinkMessage) extends PutRequest
}
