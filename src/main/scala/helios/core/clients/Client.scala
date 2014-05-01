package helios.core.clients

import akka.actor.{ActorRef, Actor}
import helios.messages.CoreMessages.ClientType


object Clients {
  type ClientTypeFactory = ActorRef => ClientType

  abstract case class Client(clientTypeFactory: ClientTypeFactory) extends Actor
}
