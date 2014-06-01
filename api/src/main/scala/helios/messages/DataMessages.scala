package helios.messages

import org.mavlink.messages.MAVLinkMessage
import akka.util.ByteString


object DataMessages {

  /**
   * Represents a MAVLink message that should be published to the rest of the system
   * @param message the message to publish
   */
  case class PublishMAVLink(message: MAVLinkMessage)

  /**
   * Represents a raw ByteString which should be published to the rest of the system
   * @param data the data to publish
   */
  case class UARTData(data: ByteString)
}