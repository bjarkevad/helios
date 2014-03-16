package helios.core.actors

import akka.actor._
import akka.io.{IO, UdpConnected}

import helios.core.actors.flightcontroller.{MockSerial, HeliosUART}
import helios.core.actors.flightcontroller.FlightControllerMessages.WriteMAVLink
import helios.core.actors.ClientHandler.BecomePrimary
import helios.api.HeliosAPI
import helios.api.messages.MAVLinkMessages.PublishMAVLink
import helios.api.HeliosApplicationDefault.RegisterAPIClient

import org.mavlink.messages.common.msg_heartbeat

import org.slf4j.LoggerFactory

object ClientReceptionist {
  def props: Props = Props(new ClientReceptionist)
}

class ClientReceptionist extends Actor {

  import CoreMessages._
  import scala.collection.mutable
  import context.system

  /** Contains a map from Handler to Client */
  val clients: mutable.HashMap[ActorRef, ActorRef] = mutable.HashMap.empty
  val logger = LoggerFactory.getLogger(classOf[ClientReceptionist])

  //TODO: Move this to config file
  val mockSerial = context.actorOf(MockSerial.props)
  val uart = context.actorOf(HeliosUART.props(self, mockSerial, null))
  //TODO: Move this to config file
  val groundControl =
    context.actorOf(GroundControl.props(IO(UdpConnected), "localhost", 14550), "GroundControl")

  context watch groundControl

  override def preStart() = {
    logger.debug(context.self.path.toString)
  }

  def receive = defaultReceive(groundControlAlive = true) orElse terminator

  def defaultReceive(groundControlAlive: Boolean): Receive = {
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
      sender ! Unregistered()

    case RegisterAPIClient(c) =>
      val hd: HeliosAPI =
        TypedActor(context.system)
          .typedActorOf(TypedProps(
          classOf[HeliosAPI], new HeliosAPIDefault("HeliosDefault", self, c)))

      clients put(TypedActor(context.system).getActorRefFor(hd), c)
      sender ! hd

    case m@PublishMAVLink(ml) =>
      //Always send heartbeats to groundcontrol
      if (groundControlAlive && !clients.contains(groundControl) && ml.isInstanceOf[msg_heartbeat])
        groundControl ! m

      clients.keys foreach (_ ! m)

    case WriteMAVLink(m) =>
      //TODO: Permissions
      uart ! WriteMAVLink(m)

    case TestMsg =>
      logger.debug("YAY API WORKS!")

    case _ => sender ! NotRegistered(sender)
  }

  def terminator: Receive = {
    case Terminated(a) if clients contains a =>
      clients remove a
      clients(a) ! Unregistered()

    case Terminated(`groundControl`) =>
      context.become(defaultReceive(groundControlAlive = false) orElse terminator)

    case Terminated(a) =>
      logger.debug("Unhandled terminated message")

  }
}


object CoreMessages {

  import SubscriptionTypes._

  trait Request

  trait Response

  trait PutRequest

  case class TestMsg()

  case class RegisterClient(client: ActorRef) extends Request

  case class UnregisterClient(client: ActorRef) extends Request

  case class Registered(client: ActorRef) extends Response

  case class RegisteredAPI(helios: HeliosAPI) extends Response

  case class Unregistered() extends Response

  case class NotRegistered(client: ActorRef) extends Response

  case class Subscribe(subType: SubscriptionType) extends Request

  case class Unsubscribe(subType: SubscriptionType) extends Request

  case class NotAllowed() extends Response

}

object SubscriptionTypes {

  trait SubscriptionType

  case class SystemStatus() extends SubscriptionType

}