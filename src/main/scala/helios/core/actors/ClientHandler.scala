package helios.core.actors

import akka.actor.{Props, ActorRef, Actor}
import helios.core.actors.ClientHandler.{BecomePrimary, BecomeSecondary}
import org.slf4j.LoggerFactory
import org.mavlink.messages.MAVLinkMessage
import helios.core.actors.flightcontroller.FlightControllerMessages.WriteMAVLink
import helios.api.messages.MAVLinkMessages.PublishMAVLink
import helios.core.actors.CoreMessages.NotAllowed

trait MAVLinkHandler {
  def handle(msg: MAVLinkMessage): Option[List[MAVLinkMessage]]
  def handleSecondary(msg: MAVLinkMessage): Option[List[MAVLinkMessage]]

}

class DefaultMAVLinkHandler(uas: ActorRef) extends MAVLinkHandler {
  override def handle(msg: MAVLinkMessage): Option[List[MAVLinkMessage]] = {
    ???
  }

  override def handleSecondary(msg: MAVLinkMessage): Option[List[MAVLinkMessage]] = {
    ???
  }
}

object DefaultMAVLinkHandler {
  def apply(uas: ActorRef) = new DefaultMAVLinkHandler(uas)
}

object ClientHandler {
  case class BecomePrimary()
  case class BecomeSecondary()

  //TODO: Inject MAVLinkHandler
  def apply(client: ActorRef, uas: ActorRef): Props =
    Props(new ClientHandler(client, DefaultMAVLinkHandler(uas)))
}

class ClientHandler(val client: ActorRef, mlHandler: MAVLinkHandler) extends Actor {

  lazy val logger = LoggerFactory.getLogger(classOf[ClientHandler])

  override def preStart() = {
    //client ! Registered(client)
    logger.debug("Started")
  }

  override def postStop() = {
  }

  def receive: Actor.Receive = secondary

  def primary: Actor.Receive = pri orElse shared
  def secondary: Actor.Receive = primary //TODO: Everyone is primary for debugging purposes
  //def secondary: Actor.Receive = sec orElse shared

  def pri: Actor.Receive = {
    case BecomeSecondary() => context.become(secondary)

    case m@WriteMAVLink(msg) =>
      //logger.debug(s"received MAVLink: $msg")//Write to UART
      //mlHandler.handle(msg)
      context.parent ! m

    case m@PublishMAVLink(msg) =>
      client ! m
  }

  def sec: Actor.Receive = {
    case BecomePrimary() => context.become(primary)

    case m@WriteMAVLink(_) => sender ! NotAllowed()
  }

  def shared: Actor.Receive = {
    case _ =>
  }
}

