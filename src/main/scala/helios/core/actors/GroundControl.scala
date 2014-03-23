package helios.core.actors

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
import helios.HeliosConfig

object GroundControl {

  def props(udpManager: ActorRef): Props = Props(new GroundControl(udpManager))
}

class GroundControl(udpManager: ActorRef) extends Actor with Stash {

  import language.postfixOps

  lazy val logger = LoggerFactory.getLogger(classOf[GroundControl])

  lazy val address: InetSocketAddress = {
    HeliosConfig.groundcontrolAddress
    .getOrElse(new InetSocketAddress("localhost", 14550))
  }

  override def preStart() = {
    logger.debug(s"Address: ${address.getHostName}: ${address.getPort}")
    udpManager ! UdpConnected.Connect(self, address)
    context.parent ! RegisterClient(self)
  }

  override def postStop() = {}

  override def receive: Receive = unbound()

  def unbound(connection: Option[ActorRef] = None, handler: Option[ActorRef] = None): Receive = {
    case UdpConnected.Connected =>
      val con = sender
      handler match {
        case Some(h) =>
          context become registered(con, h)
        case None =>
          context become unbound(Some(con), handler)
      }

    case UdpConnected.CommandFailed(cmd: UdpConnected.Connect) =>
      logger.warn(s"Groundcontrol could not connect to ${address.getHostName}:${address.getPort}")
      self ! PoisonPill

    case Registered(handler) =>
      connection match {
        case Some(c) =>
          context become registered(c, handler)
        case None =>
          context become unbound(connection, Option(handler))
      }
  }

  def registered(connection: ActorRef, handler: ActorRef): Receive = {
    case msg@UdpConnected.Received(v) =>
      convertToMAVLink(v) match {
        case Success(m: MAVLinkMessage) =>
          //logger.debug(s"received MAVLink: $m")
          handler ! WriteMAVLink(m)

        case Failure(e: Throwable) =>
          logger.warn(s"received an unknown message over UDP")
      }

    case msg@UdpConnected.CommandFailed(cmd) =>
      connection ! cmd //RESEND MOFO

    case msg@PublishMAVLink(ml) =>
      connection ! UdpConnected.Send(ByteString(ml.encode()))

    case d@UdpConnected.Disconnect =>
      connection ! d

    case UdpConnected.Disconnected =>
      self ! PoisonPill

    case m@_ => logger.warn(s"received something unexpected: $m")
  }
}