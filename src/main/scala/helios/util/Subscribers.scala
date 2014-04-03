package helios.util

import akka.actor.ActorRef

object Subscribers {
  implicit class subscriberImpls(val subs: Set[ActorRef]) {
    def !(msg: Any)(implicit sender: ActorRef) = subs.foreach(_ ! msg)
  }
}
