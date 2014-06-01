package helios.core.clients

import akka.actor.{ActorRef, Actor}
import helios.types.ClientTypes.ClientType

object Clients {
  /**
   * Type alias for a factory function
   */
  type ClientTypeProvider = ActorRef => ClientType

  /**
   * Base trait used for all Clients in the system, only contains definition of the client's type
   */
  trait Client extends Actor {
    val clientTypeProvider: ClientTypeProvider
    lazy val clientType = clientTypeProvider(self)
  }
}