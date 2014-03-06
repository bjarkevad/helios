package helios.apimessages

import akka.actor.ActorRef


trait Request
trait Response

trait PutRequest

object CoreMessages  {
  case class RegisterClient(client: ActorRef) extends Request
  case class UnregisterClient(client: ActorRef) extends Request

  case class Registered(client: ActorRef) extends Response
  case class Unregistered(client: ActorRef) extends Response

  case class NotRegistered(client: ActorRef) extends Response

  case class Subscribe(subType: SubscriptionType) extends Request
  case class Unsubscribe(subType: SubscriptionType) extends Request

  case class NotAllowed(value: PutRequest) extends Response
}
