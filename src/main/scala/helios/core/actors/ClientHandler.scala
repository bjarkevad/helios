package helios.core.actors

import akka.actor.{Props, ActorRef, Actor}
import helios.apimessages.CoreMessages.{NotAllowed, UnregisterClient, Registered}
import helios.core.actors.ClientHandler.{BecomePrimary, BecomeSecondary}
import helios.apimessages.MAVLinkMessages.RawMAVLink
import scala.concurrent.ExecutionContext.Implicits.global
import org.slf4j.LoggerFactory

object ClientHandler {
  case class BecomePrimary()
  case class BecomeSecondary()

  def apply(client: ActorRef): Props = Props(new ClientHandler(client))
}

class ClientHandler(val client: ActorRef) extends Actor {

  lazy val logger = LoggerFactory.getLogger(classOf[ClientHandler])

  override def preStart() = {
    client ! Registered(client)
    logger.debug("Started")
  }

  override def postStop() = {
  }

  def receive: Actor.Receive = secondary

  def primary: Actor.Receive = pri orElse shared
  def secondary: Actor.Receive = sec orElse shared

  def pri: Actor.Receive = {
    case BecomeSecondary() => context.become(secondary)

    case RawMAVLink(msg) => logger.debug(s"received MAVLink: $msg")//Write to UART
  }

  def sec: Actor.Receive = {
    case BecomePrimary() => context.become(primary)

    case m@RawMAVLink(_) => sender ! NotAllowed(m)
  }

  def shared: Actor.Receive = {
    case _ =>
  }
}
