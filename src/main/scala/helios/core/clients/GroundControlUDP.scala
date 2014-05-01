package helios.core.clients

import akka.actor._
import akka.io.UdpConnected
import akka.util.ByteString

import org.mavlink.messages._

import java.net.InetSocketAddress
import scala.util.{Success, Failure}
import org.slf4j.LoggerFactory
import helios.messages.DataMessages.PublishMAVLink
import helios.util.mavlink.MAVLink.convertToMAVLink
import helios.messages.CoreMessages.{Registered, RegisterClient}
import Clients._
import helios.types.Subscribers
import helios.core.clients.DataMessages.WriteMAVLink

object GroundControlUDP {
  def props(clientTypeFactory: ClientTypeFactory, udpManager: ActorRef, address: InetSocketAddress): Props =
    Props(new GroundControlUDP(clientTypeFactory, udpManager, address))
}

class GroundControlUDP(clientTypeFactory: ClientTypeFactory, udpManager: ActorRef, address: InetSocketAddress)
  extends Client(clientTypeFactory) with Stash {

  import language.postfixOps
  import Subscribers._

  lazy val logger = LoggerFactory.getLogger(classOf[GroundControlUDP])

  override def preStart() = {
    logger.debug(s"Address: ${address.getHostName}: ${address.getPort}")
    udpManager ! UdpConnected.Connect(self, address)
  }

  override def receive: Receive = unbound()

  def publishTargets(subscribers: Subscribers): Subscribers = {
    ???
  }

  def unbound(connection: Option[ActorRef] = None): Receive = {
    case UdpConnected.Connected =>
      val connection = sender
      context.parent ! RegisterClient(clientTypeFactory(self))
      context become awaitingRegistration(connection)

    case UdpConnected.CommandFailed(cmd: UdpConnected.Connect) =>
      logger.warn(s"Groundcontrol could not connect to ${address.getHostName}:${address.getPort}")
      self ! PoisonPill

    case _ =>
      stash()
  }

  def awaitingRegistration(connection: ActorRef): Receive = {
    case Registered(ct) =>
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
          publishTargets(subscribers) ! PublishMAVLink(m)

        case Failure(e: Throwable) =>
          logger.warn(s"received an unknown message over UDP")
      }

    case msg@UdpConnected.CommandFailed(cmd) =>
      connection ! cmd

    case msg@WriteMAVLink(ml) =>
      connection ! UdpConnected.Send(ByteString(ml.encode()))

    case d@UdpConnected.Disconnect =>
      connection ! d

    case UdpConnected.Disconnected =>
      context stop self

    case m@_ =>
      logger.debug(s"Received something unhandled: $m")
  }

}