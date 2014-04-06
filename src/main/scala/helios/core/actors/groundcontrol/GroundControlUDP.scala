package helios.core.actors.groundcontrol

import akka.actor._
import akka.io.UdpConnected
import akka.util.ByteString

import org.mavlink.messages._

import java.net.InetSocketAddress
import scala.util.{Success, Failure}
import org.slf4j.LoggerFactory
import helios.api.messages.MAVLinkMessages.PublishMAVLink
import helios.core.actors.CoreMessages._
import helios.mavlink.MAVLink.convertToMAVLink

object GroundControlUDP {
  def props(udpManager: ActorRef, address: InetSocketAddress): Props = Props(new GroundControlUDP(udpManager, address))
}

class GroundControlUDP(udpManager: ActorRef, address: InetSocketAddress) extends Actor with Stash {

  import language.postfixOps
  import helios.util.Subscribers._

  lazy val logger = LoggerFactory.getLogger(classOf[GroundControlUDP])

  override def preStart() = {
    logger.debug(s"Address: ${address.getHostName}: ${address.getPort}")
    udpManager ! UdpConnected.Connect(self, address)
  }

  override def receive: Receive = unbound()

  def unbound(connection: Option[ActorRef] = None): Receive = {
    case UdpConnected.Connected =>
      val connection = sender
      context.parent ! RegisterClient(self)
      context become awaitingRegistration(connection)

    case UdpConnected.CommandFailed(cmd: UdpConnected.Connect) =>
      logger.warn(s"Groundcontrol could not connect to ${address.getHostName}:${address.getPort}")
      self ! PoisonPill

    case _ =>
      stash()
  }

  def awaitingRegistration(connection: ActorRef): Receive = {
    case Registered(handler) =>
      context become registered(connection)
      unstashAll() //Unstash from unbound & awaitingRegistration at once

    case _ =>
      stash()
  }

  def registered(connection: ActorRef, subscribers: Subscribers = NoSubscribers): Receive = {
    case msg@UdpConnected.Received(v) =>
      convertToMAVLink(v) match {
        case Success(m: MAVLinkMessage) =>
          m.componentId = 1
          subscribers ! PublishMAVLink(m)

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
      context become registered(connection, subs)

    case m@_ => logger.debug(s"received something unexpected: $m")
  }

}