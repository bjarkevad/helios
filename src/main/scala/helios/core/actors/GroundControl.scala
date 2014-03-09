package helios.core.actors

import akka.actor._
import akka.io.UdpConnected
import akka.util.ByteString

import org.mavlink.messages._
import org.mavlink.messages.common.msg_heartbeat

import helios.mavlink.MAVLink
import helios.apimessages.MAVLinkMessages._
import MAVLink._
import java.net.InetSocketAddress
import scala.util.{Success, Failure}
import org.slf4j.LoggerFactory

object GroundControl {
  def props(udpManager: ActorRef): Props = Props(new GroundControl(udpManager))
}

class GroundControl(udpManager: ActorRef) extends Actor with Stash {

  import helios.apimessages.CoreMessages._
  import concurrent.duration._
  import language.postfixOps
  import scala.concurrent.ExecutionContext.Implicits.global

  //TODO: heartbeat needs to be sent when it's ready from HeliosUART
  lazy val heartbeat: ByteString = {
    val hb = new msg_heartbeat(20, MAV_COMPONENT.MAV_COMP_ID_IMU)
    hb.sequence = 0
    hb.`type` = MAV_TYPE.MAV_TYPE_QUADROTOR
    hb.autopilot = MAV_AUTOPILOT.MAV_AUTOPILOT_GENERIC
    hb.base_mode = MAV_MODE_FLAG.MAV_MODE_FLAG_AUTO_ENABLED //MAV_MODE.MAV_MODE_PREFLIGHT
    hb.custom_mode = 0
    hb.system_status = MAV_STATE.MAV_STATE_STANDBY
    hb.mavlink_version = 3

    ByteString(hb.encode())
  }
  val heartbeatFreq: FiniteDuration = 1.second
  //

  lazy val logger = LoggerFactory.getLogger(classOf[GroundControl])

  override def preStart() = {
    logger.debug("Started")
    udpManager ! UdpConnected.Connect(self, new InetSocketAddress("localhost", 14550))
  }

  override def postStop() = {}

  override def receive: Receive = unbound

  def unbound: Receive = _unbound orElse unknown

  def unregistered(connection: ActorRef): Receive =
    _unregistered(connection) orElse sharedUdp(connection) orElse unknown

  def awaitingRegistration(connection: ActorRef, registerTask: Cancellable): Receive =
    _awaitingRegistration(connection, registerTask) orElse sharedUdp(connection) orElse unknown

  def registered(connection: ActorRef, handler: ActorRef): Receive =
    _registered(connection, handler) orElse sharedUdp(connection) orElse unknown

  def _unbound: Receive = {
    case UdpConnected.Connected =>
      context become unregistered(sender)
      context.system.scheduler.schedule(50 millis, heartbeatFreq, sender, UdpConnected.Send(heartbeat))
      logger.debug("UdpConnected.Connected")
  }

  def _unregistered(connection: ActorRef): Receive = {
    //Only register with Receptionist when a message is received from the GC
    case msg@UdpConnected.Received(v) =>
      stash()
      val reg = context.system.scheduler.schedule(0 millis, 100 millis, context.parent, RegisterClient(self))
      context become awaitingRegistration(connection, reg)
      logger.debug("became 'awaitingRegistration'")
  }

  def _awaitingRegistration(connection: ActorRef, registerTask: Cancellable): Receive = {
    case msg@UdpConnected.Received(v) =>
      stash()
      logger.debug("added message to stash")

    case Registered(c) if c == self =>
      unstashAll()
      registerTask.cancel()
      context become registered(connection, sender)
      logger.debug("became 'registered'")

    case Registered(c) =>
      logger.debug("received Registered() with wrong parameter")
  }

  def _registered(connection: ActorRef, handler: ActorRef): Receive = {
    case msg@UdpConnected.Received(v) =>
      convertToMAVLink(v) match {
        case Success(m: MAVLinkMessage) =>
          logger.debug(s"received MAVLink: $m")
          handler ! RawMAVLink(m)

        case Failure(e: Throwable) =>
          logger.warn(s"received an unknown message over UDP")

        case _ =>
          logger.warn("huh")
      }
  }

  def sharedUdp(connection: ActorRef): Receive = {
    case d@UdpConnected.Disconnect => connection ! d

    case UdpConnected.Disconnected => self ! PoisonPill
  }

  def unknown: Receive = {
    case m@_ => logger.warn(s"received something unexpected: $m")
  }
}
