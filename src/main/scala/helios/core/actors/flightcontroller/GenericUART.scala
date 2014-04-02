package helios.core.actors.flightcontroller

import akka.actor.{ActorRef, Actor}

object GenericUART {

}

class GenericUART(uartManager: ActorRef) extends Actor {

  override def receive: Receive = {
    case _ =>

  }
}
