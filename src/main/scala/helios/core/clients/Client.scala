package helios.core.clients

import akka.actor.{ActorRef, Actor}
import helios.types.ClientTypes.ClientType

object Clients {
  type ClientTypeProvider = ActorRef => ClientType

  trait Client extends Actor {
    val clientTypeProvider: ClientTypeProvider
    lazy val clientType = clientTypeProvider(self)
  }
}
