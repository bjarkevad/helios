package helios.messages

import org.mavlink.messages.MAVLinkMessage
import akka.util.ByteString


object DataMessages {
  case class PublishMAVLink(message: MAVLinkMessage)
  case class UARTData(data: ByteString)
}