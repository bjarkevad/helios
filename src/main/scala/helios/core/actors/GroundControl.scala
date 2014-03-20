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

object GroundControl {
  def props(udpManager: ActorRef): Props = Props(new GroundControl(udpManager, "localhost", 14550))

  def props(udpManager: ActorRef, hostName: String, port: Int): Props = Props(new GroundControl(udpManager, hostName, port))
}

class GroundControl(udpManager: ActorRef, hostName: String, port: Int) extends Actor with Stash {

  import language.postfixOps

  lazy val logger = LoggerFactory.getLogger(classOf[GroundControl])

  override def preStart() = {
    udpManager ! UdpConnected.Connect(self, new InetSocketAddress(hostName, port))
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
      logger.warn(s"Groundcontrol could not connect to $hostName:$port")
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