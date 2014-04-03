package helios.core.actors.groundcontrol

import akka.actor._
import akka.io.UdpConnected
import akka.util.ByteString

import org.mavlink.messages._

import helios.mavlink.MAVLink
import MAVLink._
import java.net.InetSocketAddress
import scala.util.{Success, Failure}
import org.slf4j.LoggerFactory
import helios.api.messages.MAVLinkMessages.PublishMAVLink
import helios.core.actors.flightcontroller.FlightControllerMessages.WriteMAVLink
import helios.core.actors.CoreMessages._
import helios.core.actors.flightcontroller.MAVLinkUART.SetSubscribers

object GroundControlUDP {

  def props(udpManager: ActorRef, address: InetSocketAddress): Props = Props(new GroundControlUDP(udpManager, address))
}

class GroundControlUDP(udpManager: ActorRef, address: InetSocketAddress) extends Actor with Stash {

  import language.postfixOps
  import helios.util.Subscribers._

  lazy val logger = LoggerFactory.getLogger(classOf[GroundControlUDP])

  override def preStart() = {
    logger.debug(s"Address: ${address.getHostName}: ${address.getPort}")
    logger.debug(s"Parent: ${context.parent}")
    udpManager ! UdpConnected.Connect(self, address)
    context.parent ! RegisterClient(self)
  }

  override def postStop() = {}

  override def receive: Receive = unbound()

  def unbound(connection: Option[ActorRef] = None, receivers: Option[Set[ActorRef]] = None): Receive = {
    case UdpConnected.Connected =>
      val connection = sender
      receivers match {
        case Some(h) =>
          context become registered(connection, h)
          unstashAll()
        case None =>
          context become unbound(Some(connection), receivers)
      }

    case UdpConnected.CommandFailed(cmd: UdpConnected.Connect) =>
      logger.warn(s"Groundcontrol could not connect to ${address.getHostName}:${address.getPort}")
      self ! PoisonPill

    case Registered(receivers) =>
      connection match {
        case Some(c) =>
          context become registered(c, Set())
          unstashAll()
        case None =>
          context become unbound(connection, Option(Set()))
      }

    case SetSubscribers(_) =>
      stash()
  }

  def registered(connection: ActorRef, receivers: Set[ActorRef]): Receive = {
    case msg@UdpConnected.Received(v) =>
      convertToMAVLink(v) match {
        case Success(m: MAVLinkMessage) =>
          m.componentId = 1
          logger.debug(s"received MAVLink: $m componentId: ${m.componentId} sending to ${receivers.size} subscribers")
          receivers ! PublishMAVLink(m)

        case Failure(e: Throwable) =>
          logger.warn(s"received an unknown message over UDP")
      }

    case msg@UdpConnected.CommandFailed(cmd) =>
      connection ! cmd

    case msg@PublishMAVLink(ml) =>
      connection ! UdpConnected.Send(ByteString(ml.encode()))

    case d@UdpConnected.Disconnect =>
      connection ! d

    case UdpConnected.Disconnected =>
      self ! PoisonPill

    case SetSubscribers(subs) =>
      logger.debug(s"Updating subscriptions, new count: ${subs.size}")
      context become registered(connection, subs)

    case m@_ => logger.warn(s"received something unexpected: $m")
  }

}