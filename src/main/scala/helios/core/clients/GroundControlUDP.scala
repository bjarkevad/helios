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
import helios.messages.CoreMessages.{SetSubscribers, UnregisterClient, Registered, RegisterClient}
import Clients._
import helios.types.Subscribers
import helios.core.clients.DataMessages.WriteMAVLink

object GroundControlUDP {
  def props(clientTypeProvider: ClientTypeProvider, udpManager: ActorRef, address: InetSocketAddress): Props =
    Props(new GroundControlUDP(clientTypeProvider, udpManager, address))
}

/**
 * A Client used to connect to UDP
 * @param clientTypeProvider the type provider function used to indicate the type of the client
 * @param udpManager the system's UDP manager actor
 * @param address the IP address to bind to
 */
class GroundControlUDP(val clientTypeProvider: ClientTypeProvider, udpManager: ActorRef, address: InetSocketAddress)
  extends Client with Stash {

  import language.postfixOps
  import Subscribers._

  lazy val logger = LoggerFactory.getLogger(classOf[GroundControlUDP])

  override def preStart() = {
    logger.debug(s"Address: ${address.getHostName}: ${address.getPort}")
    udpManager ! UdpConnected.Connect(self, address)
  }

  override def postStop() = {
    context.parent ! UnregisterClient(clientType)
  }

  override def receive: Receive = unbound()

  /**
   * Filters subscribers to only the ones interested in messages from this Client, everyone for now
   * @param subscribers the subscribers to filter
   * @return the filtered subscribers
   */
  def publishTargets(subscribers: Subscribers): Subscribers = subscribers

  /**
   * The client's unbound, initial state. Not connected to the UDP socket yet
   * @param connection the connection
   * @return
   */
  def unbound(connection: Option[ActorRef] = None): Receive = {
    case UdpConnected.Connected =>
      val connection = sender
      context.parent ! RegisterClient(clientType)
      context become awaitingRegistration(connection)

    case UdpConnected.CommandFailed(cmd: UdpConnected.Connect) =>
      logger.warn(s"Groundcontrol could not connect to ${address.getHostName}:${address.getPort}")
      self ! PoisonPill

    case _ =>
      stash()
  }

  /**
   * Waiting for registration with the Receptionist
   * @param connection the connection
   * @return
   */
  def awaitingRegistration(connection: ActorRef): Receive = {
    case Registered(ct) =>
      context become registered(connection)
      unstashAll() //Unstash from unbound & awaitingRegistration at once

    case _ =>
      stash()
  }

  /**
   * The final state where the client is connected and registered
   * @param connection the connection
   * @param subscribers the subscribers
   * @return
   */
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

    case SetSubscribers(subs) =>
      context become registered(connection, subs ++ subscribers)

    case m@_ =>
      logger.debug(s"Received something unhandled: $m")
  }

}