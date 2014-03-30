package helios.core.actors

import akka.actor._
import akka.io.{IO, UdpConnected}
import akka.actor.SupervisorStrategy._

import language.postfixOps
import concurrent.duration._

import helios.core.actors.flightcontroller.{MockSerial, HeliosUART}
import helios.core.actors.flightcontroller.FlightControllerMessages.WriteMAVLink
import helios.core.actors.flightcontroller.HeliosUART.SetPrimary
import helios.api.HeliosAPI
import helios.api.messages.MAVLinkMessages.PublishMAVLink
import helios.api.HeliosApplicationDefault.RegisterAPIClient
import helios.HeliosConfig

import org.slf4j.LoggerFactory
import com.github.jodersky.flow.Serial

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

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 5,
    withinTimeRange = 5 seconds,
    loggingEnabled = true) {
    case _: java.io.IOException => Restart
    case _: NotImplementedError => Resume
    case e =>
      logger.warn("Unhandled supervisor event")
      Restart
  }

  lazy val uartManager: ActorRef = {
    HeliosConfig.serialdevice match {
      case Some("MOCK") => context.actorOf(MockSerial.props)
      case Some(_) => IO(Serial)
      case None => context.actorOf(MockSerial.props)
    }
  }

  val uart = context.actorOf(HeliosUART.props(self, uartManager))

  val groundControl = context.actorOf(GroundControl.props(IO(UdpConnected)), "GroundControl")

  context watch groundControl

  override def preStart() = {
    logger.debug(context.self.path.toString)
  }

  def receive = defaultReceive(groundControlUnregistered = true) orElse terminator

  def defaultReceive(groundControlUnregistered: Boolean): Receive = {
    case RegisterClient(c) =>
      val ch = context.actorOf(ClientHandler.props(c, uart))

      if (clients.isEmpty)
        uart ! SetPrimary(c)

      clients put(ch, c)

      c ! Registered(ch)

    case UnregisterClient(c) if clients contains c =>
      clients remove c
      sender ! Unregistered()

    case RegisterAPIClient(c) =>
      val hd: HeliosAPI =
        TypedActor(context.system)
          .typedActorOf(TypedProps(
          classOf[HeliosAPI], new HeliosAPIDefault("HeliosDefault", self, c, uart, 20)))

      if (clients.isEmpty)
        uart ! SetPrimary(c)

      clients put(TypedActor(context.system).getActorRefFor(hd), c)

      sender ! hd

    case m@PublishMAVLink(ml) =>
      //logger.debug(s"Publishing MAVLink: $ml")
      clients.keys foreach (_ ! m)

    case WriteMAVLink(m) =>
      logger.error("ClientReceptionist should not receive WriteMAVLink messages!!")

    case _ =>
      sender ! NotRegistered(sender)
  }

  def terminator: Receive = {
    case Terminated(a) if clients contains a =>
      clients remove a
      clients(a) ! Unregistered()

    case Terminated(`groundControl`) =>
      context.become(defaultReceive(groundControlUnregistered = false) orElse terminator)

    case Terminated(a) =>
      logger.debug("Unhandled terminated message")
  }
}

object CoreMessages {

  import SubscriptionTypes._

  trait Request

  trait Response

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

//TODO: Remove or what?
object SubscriptionTypes {

  trait SubscriptionType

  case class SystemStatus() extends SubscriptionType

}