package helios.core.actors

import akka.actor._

import scala.collection.mutable

import helios.apimessages.CoreMessages._

import org.slf4j.LoggerFactory
import akka.io.{IO, UdpConnected}
import org.mavlink.messages.MAVLinkMessage
import helios.core.actors.flightcontroller.{MockSerial, HeliosUART}
import helios.apimessages.CoreMessages.RegisterClient
import helios.apimessages.CoreMessages.NotRegistered
import helios.core.actors.ClientHandler.BecomePrimary
import helios.apimessages.CoreMessages.UnregisterClient
import helios.apimessages.CoreMessages.Unregistered
import helios.apimessages.MAVLinkMessages.RawMAVLink
import akka.actor.Terminated
import helios.core.actors.flightcontroller.FlightControllerMessages.WriteMAVLink
import scala.Some
import helios.apimessages.CoreMessages.TestMsg
import helios.api.HeliosAPI
import helios.core.actors.ClientReceptionist.PublishMAVLink

object ClientReceptionist {
  case class PublishMAVLink(message: MAVLinkMessage)

  def props: Props = Props(new ClientReceptionist)
}

class ClientReceptionist extends Actor {

  /** Contains a map from Handler to Client */
  val clients: mutable.HashMap[ActorRef, ActorRef] = mutable.HashMap.empty

  val logger = LoggerFactory.getLogger(classOf[ClientReceptionist])

  val mockSerial = context.actorOf(MockSerial.props)
  val uart = context.actorOf(HeliosUART.props(self, mockSerial, null))

  override def preStart() = {
    import context.system
    context.actorOf(GroundControl.props(IO(UdpConnected)), "GroundControl")
    logger.debug(context.self.path.toString)
  }

  def receive: Receive = {
    case RegisterClient(c) =>
      val ch = context.actorOf(ClientHandler(c, self))

      //First clienthandler is primary
      if (clients.isEmpty) {
        ch ! BecomePrimary()
      }

      clients put(ch, c) match {
        case None => //Entry is new
        case Some(_) => //Entry already exists
      }

      c ! Registered(ch)

    case UnregisterClient(c) if clients contains c =>
      clients remove c
      sender ! Unregistered(c)

    case RegisterAPIClient(c) =>
      val hd: HeliosAPI =
        TypedActor(context.system)
          .typedActorOf(TypedProps(
          classOf[HeliosAPI], new HeliosAPIDefault("HeliosDefault", self, c)))

      clients put(TypedActor(context.system).getActorRefFor(hd), c)
      sender ! hd

    case m@PublishMAVLink(ml) =>
    //Everyone gets everything..
      clients.keys foreach (_ ! m)

    case RawMAVLink(m) =>
      //Send to UART
      uart ! WriteMAVLink(m)

    case Terminated(a) if clients contains a =>
      val client = clients(a)
      client ! Unregistered(client)

    case Terminated(a) =>
      logger.debug("Unhandled terminated message")

    case TestMsg =>
      logger.debug("YAY API WORKS!")

    case _ => sender ! NotRegistered(sender)
  }
}
