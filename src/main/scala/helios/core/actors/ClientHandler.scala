package helios.core.actors

import akka.actor.{Props, ActorRef, Actor}
import org.slf4j.LoggerFactory
import helios.core.actors.flightcontroller.FlightControllerMessages.WriteMAVLink
import helios.api.messages.MAVLinkMessages.PublishMAVLink
import helios.core.actors.flightcontroller.MAVLinkUART

object ClientHandler {
  def props(client: ActorRef, uart: ActorRef): Props =
    Props(new ClientHandler(client, uart))
}

class ClientHandler(client: ActorRef, uart: ActorRef) extends Actor {

  lazy val logger = LoggerFactory.getLogger(classOf[ClientHandler])

  override def preStart() = {
    //client ! Registered(client)
    logger.debug("Started")
  }

  override def postStop() = {
  }

  def receive: Actor.Receive = {
    case m@WriteMAVLink(msg) =>
      //logger.debug(s"received MAVLink: $msg")//Write to UART
      //mlHandler.handle(msg)
      uart ! m

    case m@PublishMAVLink(msg) =>
      client ! m

    case MAVLinkUART.NotAllowed(msg) =>
      logger.warn(s"Tried to write $msg with insufficient permissions")

    case _ =>
  }

}
