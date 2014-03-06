package helios.core.actors

import akka.actor._
import akka.io.{UdpConnected, IO}
import org.mavlink.messages._
import org.mavlink.messages.common.msg_heartbeat
import org.mavlink.MAVLinkReader
import java.net.InetSocketAddress
import scala.collection.mutable
import scala.util.{Success, Failure, Try}
import helios.apimessages.MAVLinkMessages._
import org.slf4j.LoggerFactory
import akka.util.ByteString

object GroundControl {
  def apply(): Props = Props(classOf[GroundControl])
}

class GroundControl extends Actor {

  import helios.apimessages.CoreMessages._
  import concurrent.duration._
  import language.postfixOps
  import scala.concurrent.ExecutionContext.Implicits.global
  import context.system

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
  lazy val packetCache: mutable.Buffer[UdpConnected.Received] = mutable.Buffer.empty
  lazy val logger = LoggerFactory.getLogger(classOf[GroundControl])
  lazy val udpManager = IO(UdpConnected)
  lazy val mlReader = new MAVLinkReader()

  def convertToMAVLink(buffer: ByteString): Try[MAVLinkMessage] =
    Try(mlReader.getNextMessage(buffer.toArray, buffer.length))

  override def preStart() = {
    logger.debug("Started")
    udpManager ! UdpConnected.Connect(self, new InetSocketAddress("localhost", 14550))
  }

  override def postStop() = {

  }

  def unbound: Actor.Receive = unbd orElse unknown

  def unregistered(connection: ActorRef): Actor.Receive =
    unReg(connection) orElse sharedUdp(connection) orElse unknown

  def awaitingRegistration(connection: ActorRef, registerTask: Cancellable): Actor.Receive =
    awReg(connection, registerTask) orElse sharedUdp(connection) orElse unknown

  def registered(connection: ActorRef, handler: ActorRef): Actor.Receive =
    reg(connection, handler) orElse sharedUdp(connection) orElse unknown

  def receive: Actor.Receive = unbound

  def unbd: Actor.Receive = {
    case UdpConnected.Connected =>
      context become unregistered(sender)
      context.system.scheduler.schedule(0 millis, heartbeatFreq, sender, UdpConnected.Send(heartbeat))
      logger.debug("UdpConnected.Connected")
  }

  def unReg(connection: ActorRef): Actor.Receive = {
    case msg@UdpConnected.Received(v) =>
      val reg = context.system.scheduler.schedule(0 millis, 100 millis, context.parent, RegisterClient(self))
      context become awaitingRegistration(connection, reg)
      logger.debug("became 'awaitingRegistration'")
      self ! msg
  }

  def awReg(connection: ActorRef, registerTask: Cancellable): Actor.Receive = {
    case msg@UdpConnected.Received(v) =>
      packetCache append msg
      logger.debug("added message to cache")

    case Registered(c) if c == self =>
      registerTask.cancel()
      context become registered(connection, sender)
      logger.debug("became 'registered'")
      packetCache foreach (self ! _)
      packetCache clear()

    case Registered(c) => logger.debug("received Registered() with wrong parameter")
  }

  def reg(connection: ActorRef, handler: ActorRef): Actor.Receive = {
    case msg@UdpConnected.Received(v) =>
      convertToMAVLink(v) match {
        case Success(m) => handler ! RawMAVLink(m)
        case Failure(e) => logger.warn(s"received an unknown message over UDP: $v")
      }
  }

  def sharedUdp(connection: ActorRef): Actor.Receive = {
    case d@UdpConnected.Disconnect => connection ! d

    case UdpConnected.Disconnected => self ! PoisonPill
  }

  def unknown: Actor.Receive = {
    case m@_ => logger.warn(s"received something unexpected: $m")
  }
}
