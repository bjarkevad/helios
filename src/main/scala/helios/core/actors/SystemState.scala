/* Copyright 2014 Bjarke Vad Andersen, <bjarke.vad90@gmail.com> */

package helios.core.actors

import akka.actor.{Props, ActorRef, Actor}
import org.mavlink.messages.common.msg_heartbeat

object SystemState {
  case class HeartBeat(msg: msg_heartbeat)

  def apply(stateReceiver: ActorRef): Props = Props(new SystemState(stateReceiver))
}

class SystemState(stateReceiver: ActorRef) extends Actor {

  var newestHeartbeat: Option[msg_heartbeat] = None

  override def receive: Actor.Receive = {
    ???
  }

}
