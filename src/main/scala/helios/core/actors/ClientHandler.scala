package helios.core.actors

import akka.actor.{Props, ActorRef, Actor}
import org.slf4j.LoggerFactory
import helios.core.actors.uart.DataMessages.WriteMAVLink
import helios.api.messages.MAVLinkMessages.PublishMAVLink
import helios.core.actors.uart.MAVLinkUART
import helios.core.actors.CoreMessages.NotAllowed

object ClientHandler {
  def props(client: ActorRef, uart: ActorRef): Props =
    Props(new ClientHandler(client, uart))
}

class ClientHandler(client: ActorRef, uart: ActorRef) extends Actor {

  lazy val logger = LoggerFactory.getLogger(classOf[ClientHandler])

  def receive: Actor.Receive = {
    case m@WriteMAVLink(msg) =>
      uart ! m

    case m@PublishMAVLink(msg) =>
      if(sender != client)
        client ! m

    case NotAllowed(msg) =>
      logger.warn(s"Tried to write $msg with insufficient permissions")

    case m@_ =>
      if(sender != client)
        client ! m
  }
}
