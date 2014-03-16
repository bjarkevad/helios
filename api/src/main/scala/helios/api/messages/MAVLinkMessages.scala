package helios.api.messages

import org.mavlink.messages.MAVLinkMessage


object MAVLinkMessages {
  //MAVLink message that should be published to clients of the system (API, Groundcontrol)
  case class PublishMAVLink(message: MAVLinkMessage)
}