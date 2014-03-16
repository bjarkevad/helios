package helios.core.actors

import akka.actor._
import akka.io.UdpConnected
import akka.util.ByteString

import org.mavlink.messages._
import org.mavlink.messages.common._

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
    udpManager ! UdpConnected.Connect(self, new InetSocketAddress(hostName, port))
  }

  override def postStop() = {}

  override def receive: Receive = unbound

  def unbound: Receive = _unbound orElse unknown


  def awaitingRegistration(connection: ActorRef, registerTask: Cancellable): Receive =
    _awaitingRegistration(connection, registerTask) orElse sharedUdp(connection) orElse unknown

  def registered(connection: ActorRef, handler: ActorRef): Receive =
    _registered(connection, handler) orElse sharedUdp(connection) orElse unknown

  def _unbound: Receive = {
    case UdpConnected.Connected =>
      val con = sender()
      val reg = context.system.scheduler.schedule(0 millis, 100 millis, context.parent, RegisterClient(self))
      context become awaitingRegistration(con, reg)

      //context.system.scheduler.schedule(0 millis, heartbeatFreq, con, UdpConnected.Send(heartbeat)) //NOTE: heartbeat comes from flightcontroller
      //con ! UdpConnected.Send(heartbeat) //TODO: Move this somewhere else, only for QGC to register client

      logger.debug("UdpConnected.Connected")
      logger.debug("became 'awaitingRegistration'")

    case UdpConnected.CommandFailed(cmd: UdpConnected.Connect) =>
      logger.warn(s"Groundcontrol could not connect to $hostName:$port")
      self ! PoisonPill
  }

  def _awaitingRegistration(connection: ActorRef, registerTask: Cancellable): Receive = {
    case msg@UdpConnected.Received(v) =>
      stash()
      logger.debug("added message to stash")

    case Registered(handler) =>
      unstashAll()
      registerTask.cancel()
      context become registered(connection, handler)
      logger.debug("became 'registered'")

    //    case Registered(c) =>
    //      logger.debug("received Registered() with wrong parameter")

    case msg@PublishMAVLink(ml) =>
      connection ! UdpConnected.Send(ByteString(ml.encode()))
      //logger.debug(s"Sending MAVLink to GC: $ml")

  }

  def _registered(connection: ActorRef, handler: ActorRef): Receive = {
    case msg@UdpConnected.Received(v) =>
      convertToMAVLink(v) match {
        case Success(m: MAVLinkMessage) =>
          logger.debug(s"received MAVLink: $m")
          handler ! WriteMAVLink(m)

        case Failure(e: Throwable) =>
          logger.warn(s"received an unknown message over UDP")

        case _ =>
          logger.warn("huh")
      }

    case msg@UdpConnected.CommandFailed(cmd) =>
      connection ! cmd //RESEND MOFO

    case msg@PublishMAVLink(ml) =>
      connection ! UdpConnected.Send(ByteString(ml.encode()))
      //logger.debug("GroundControl received MAVLink")
      //logger.debug(s"Sending MAVLink to GC: $ml")

  }

  def sharedUdp(connection: ActorRef): Receive = {
    case d@UdpConnected.Disconnect => connection ! d

    case UdpConnected.Disconnected => self ! PoisonPill
  }

  def unknown: Receive = {
    case m@_ => logger.warn(s"received something unexpected: $m")
  }
}
