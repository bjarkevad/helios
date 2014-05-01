package helios.types

import akka.actor.ActorRef
import helios.types.ClientTypes.ClientType

object Subscribers {

  type Subscribers = Set[ClientType]
  lazy val NoSubscribers: Subscribers = Set.empty

  implicit class subscriberImpls(val subs: Subscribers) {
    def !(msg: Any)(implicit sender: ActorRef) = subs.foreach(_.client ! msg)
  }
}