package helios.messages

import akka.actor.ActorRef
import helios.api.HeliosAPI
import org.mavlink.messages.MAVLinkMessage
import helios.types.Subscribers.Subscribers
import helios.types.ClientTypes.ClientType

object CoreMessages {
  trait Request
  case class RegisterClient(clientType: ClientType) extends Request
  case class UnregisterClient(clientType: ClientType) extends Request
  case class RegisterAPIClient(client: ClientType) extends Request
  case class UnregisterAPIClient(client: ClientType) extends Request

  trait Response
  case class Registered(clientType: ClientType) extends Response
  case class RegisteredAPI(helios: HeliosAPI) extends Response
  case class Unregistered() extends Response
  case class SetPrimary(newPrimary: ActorRef) extends Response
  case class NotAllowed(msg: MAVLinkMessage) extends Response
  case class SetSubscribers(subscribers: Subscribers) extends Response

}
