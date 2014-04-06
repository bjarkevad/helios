package helios.util

import akka.actor.ActorRef

object Subscribers {

  type Subscribers = Set[ActorRef]
  lazy val NoSubscribers: Subscribers = Set.empty

  implicit class subscriberImpls(val subs: Subscribers) {
    def !(msg: Any)(implicit sender: ActorRef) = subs.foreach(_ ! msg)
  }
}
