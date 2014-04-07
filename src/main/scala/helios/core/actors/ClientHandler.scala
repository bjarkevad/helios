package helios.core.actors

import akka.actor.{Props, ActorRef, Actor}
import org.slf4j.LoggerFactory
import helios.core.actors.uart.DataMessages.WriteMAVLink
import helios.api.messages.MAVLinkMessages.PublishMAVLink
import helios.core.actors.CoreMessages.{SetSubscribers, NotAllowed}
import helios.util.Subscribers._

object ClientHandler {
  def props(client: ActorRef): Props =
    Props(new ClientHandler(client))
}

class ClientHandler(client: ActorRef) extends Actor {

  lazy val logger = LoggerFactory.getLogger(classOf[ClientHandler])

  def receive: Receive = defaultReceive()

  def defaultReceive(subscribers: Subscribers = NoSubscribers): Receive = {
    //Messages for the client to write
    case m@WriteMAVLink(msg) =>
      client ! m

    //Messages published to subscribers from client
    case m@PublishMAVLink(msg) =>
      subscribers ! WriteMAVLink(msg)

    case NotAllowed(msg) =>
      logger.warn(s"Tried to write $msg with insufficient permissions")

    case SetSubscribers(subs) =>
      //Make sure `self` is not included in subscribers
      context become defaultReceive(subs - self)

    case m@_ =>
      if(sender != client)
        client ! m
      else
        subscribers ! m
  }
}
