package helios.core.actors

import akka.actor._
import akka.actor.SupervisorStrategy._

import language.postfixOps
import concurrent.duration._

import helios.core.actors.uart.DataMessages.WriteMAVLink
import helios.api.HeliosAPI
import helios.api.messages.MAVLinkMessages.PublishMAVLink
import helios.api.HeliosRemote.RegisterAPIClient

import org.slf4j.LoggerFactory
import com.github.jodersky.flow.{NoSuchPortException, PortInUseException}
import helios.core.HeliosAPIDefault
import org.mavlink.messages.MAVLinkMessage
import helios.util.Subscribers.Subscribers

object ClientReceptionist {
  def props(uartProps: Props, groundControlProps: Props, muxUartProps: Props): Props =
    Props(new ClientReceptionist(uartProps, groundControlProps, muxUartProps))
}

class ClientReceptionist(uartProps: Props, groundControlProps: Props, muxUartProps: Props) extends Actor {

  import CoreMessages._
  import scala.collection.mutable

  /** Contains a map from Handler to Client */
  val clients: mutable.HashMap[ActorRef, ActorRef] = mutable.HashMap.empty
  val logger = LoggerFactory.getLogger(classOf[ClientReceptionist])

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 100,
    withinTimeRange = 1 seconds,
    loggingEnabled = true) {
    case _: java.io.IOException => Restart
    case _: PortInUseException => Restart
    case _: NoSuchPortException => Stop
    case _: NotImplementedError => Resume
    case e =>
      logger.warn("Unhandled supervisor event")
      Restart
  }

  val uart = context.actorOf(uartProps, "UART")
  val muxUart = context.actorOf(muxUartProps, "MuxUART")
  val groundControl = context.actorOf(groundControlProps, "GroundControl")

  def updateSubscriptions() {
    uart ! SetSubscribers(clients.keys.toSet)
    muxUart ! SetSubscribers(clients.keys.toSet)
    groundControl ! SetSubscribers(clients.keys.toSet)
  }

  //TODO: fix supervision strategy for UARTS
  context watch uart
  context watch muxUart
  context watch groundControl

  override def preStart() = {
    logger.debug(context.self.path.toString)
  }

  def receive = defaultReceive orElse terminator

  def defaultReceive: Receive = {
    case RegisterClient(c) =>
      val ch = context.actorOf(ClientHandler.props(c, uart))

      if (clients.isEmpty)
        uart ! SetPrimary(ch)

      clients put(ch, c)

      c ! Registered(ch)
      updateSubscriptions

    case UnregisterClient(c) if clients contains c =>
      clients remove c
      sender ! Unregistered()

    case RegisterAPIClient(c) =>
      val hd: HeliosAPI =
        TypedActor(context.system)
          .typedActorOf(TypedProps(
          classOf[HeliosAPI], new HeliosAPIDefault("HeliosDefault", self, c, uart, muxUart, 20)))

      val ch = TypedActor(context.system).getActorRefFor(hd)

      if (clients.isEmpty)
        uart ! SetPrimary(ch)

      clients put(TypedActor(context.system).getActorRefFor(hd), c)

      sender ! hd
      updateSubscriptions()

    case m@PublishMAVLink(ml) =>
      logger.error("ClientReceptionist should not receive PublishMAVLink messages!!")
      //clients.keys foreach (_ ! m)

    case WriteMAVLink(m) =>
      logger.error("ClientReceptionist should not receive WriteMAVLink messages!!")
  }

  def terminator: Receive = {
    case Terminated(a) =>
      clients remove a
      clients(a) ! Unregistered()
      updateSubscriptions()

    case m@_ =>
      logger.error(s"ClientReceptionist received: $m")
  }
}

object CoreMessages {

  trait Request

  trait Response

  case class RegisterClient(client: ActorRef) extends Request

  case class UnregisterClient(client: ActorRef) extends Request

  case class Registered(client: ActorRef) extends Response

  case class RegisteredAPI(helios: HeliosAPI) extends Response

  case class Unregistered() extends Response

  case class SetPrimary(newPrimary: ActorRef) extends Response

  case class NotAllowed(msg: MAVLinkMessage) extends Response

  case class SetSubscribers(subscribers: Subscribers) extends Response

}
