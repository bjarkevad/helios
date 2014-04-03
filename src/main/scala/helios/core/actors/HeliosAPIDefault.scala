package helios.core.actors

import akka.actor._
import scala.concurrent.Future
import org.slf4j.LoggerFactory
import helios.api.HeliosAPI
import helios.api.HeliosAPI._
import helios.api.HeliosRemote._
import helios.api.messages.MAVLinkMessages.PublishMAVLink
import helios.core.actors.flightcontroller.FlightControllerMessages.WriteMAVLink
import org.mavlink.messages._
import org.mavlink.messages.common._
import rx.lang.scala.Observable
import helios.core.actors.flightcontroller.HeliosUART.SetPrimary
import helios.core.actors.flightcontroller.HeliosUART
import java.lang.System.currentTimeMillis
import com.github.jodersky.flow.Serial
import akka.util.ByteString
import helios.core.actors.flightcontroller.MuxUART.WriteData

class HeliosAPIDefault(val name: String, val clientReceptionist: ActorRef, val client: ActorRef, val uart: ActorRef, val muxUart: ActorRef, val systemID: Int) extends HeliosAPI
with TypedActor.PreStart
with TypedActor.PostStop
with TypedActor.Receiver {

  import scala.concurrent.ExecutionContext.Implicits.global
  import HeliosAPIDefault._

  val context: ActorContext = TypedActor.context
  implicit val sender: ActorRef = context.self

  lazy val logger = LoggerFactory.getLogger(classOf[HeliosAPIDefault])

  //TODO: Move to client side
  var sysStatus: Option[SystemStatus] = None
  var sysLocation: Option[SystemPosition] = None
  var sysAttitude: Option[AttitudeRad] = None

  override def preStart() = {
    context watch client
  }

  override def postStop() = {
    logger.debug("Stopped")
    client ! PoisonPill
    clientReceptionist ! UnregisterAPIClient(TypedActor.self)
  }

  override def terminate(): Unit = {
    logger.debug("terminate called..")
    context.self ! PoisonPill
  }

  def updateSystemStatus(status: SystemStatus): Unit = {
    sysStatus = Some(status)
    client ! status
  }

  def updateSystemPosition(location: SystemPosition): Unit = {
    sysLocation = Some(location)
    client ! location
  }

  def updateSystemAttitude(attitude: AttitudeRad): Unit = {
    sysAttitude = Some(attitude)
    client ! attitude
  }

  override def onReceive(message: Any, sender: ActorRef): Unit = {
    message match {
      case m@PublishMAVLink(ml) =>
        ml.messageType match {
          case IMAVLinkMessageID.MAVLINK_MSG_ID_HEARTBEAT =>
            val hb = ml.asInstanceOf[msg_heartbeat]
            updateSystemStatus(SystemStatus(hb.`type`, hb.autopilot, hb.base_mode, hb.system_status, hb.sequence))

          case IMAVLinkMessageID.MAVLINK_MSG_ID_ATTITUDE =>
            val att = ml.asInstanceOf[msg_attitude]
            updateSystemAttitude(AttitudeRad(att.roll, att.pitch, att.yaw))

          case IMAVLinkMessageID.MAVLINK_MSG_ID_GLOBAL_POSITION_INT =>
            val pos = ml.asInstanceOf[msg_global_position_int]
            updateSystemPosition(
              SystemPosition(pos.lat, pos.lon, pos.alt, pos.relative_alt, pos.vx, pos.vy, pos.vz)
            )

          case _ =>
        }

      case Serial.Received(data) =>
        client ! UartData(data)

      case Terminated(`client`) =>
        logger.warn("Client was terminated, killing self")
        context.self ! PoisonPill

      case HeliosUART.NotAllowed(m: MAVLinkMessage) =>
        logger.warn(s"Command not allowed" +
          s"setStatus(hbdefault): $m, please call takeControl() before trying to access flight functions")

      case _ =>
        logger.warn("API received something unknown")
    }
  }

  override def ping(sent_ms: Long): Future[Long] = {
    Future(currentTimeMillis() - sent_ms)
  }

  override def writeToUart(data: ByteString): Unit = muxUart ! WriteData(data)

  override def newMission(mission: Mission): Unit = ???

  override def startMission(): Observable[MissionResult] = ???

  override def takeOff: Future[CommandResult] = Future {
    val msg = new msg_command_long(systemID, 0)
    msg.command = MAV_CMD.MAV_CMD_NAV_TAKEOFF
    msg.param1 = 0 //pitch
    msg.param4 = 0 //yaw
    msg.param5 = 0 //latitude
    msg.param6 = 0 //longtitude
    msg.param7 = 2 //altitude

    CommandSuccess()
  }

  override def land: Future[CommandResult] = Future {
    val msg = new msg_command_long(systemID, 0)
    msg.command = MAV_CMD.MAV_CMD_NAV_LAND
    msg.param4 = 0 //desired yaw
    msg.param5 = 0 //latitude
    msg.param6 = 0 //longtitude
    msg.param7 = 0 //altitude

    uart ! WriteMAVLink(msg)

    CommandSuccess()
  }

  override def rotateRight(degrees: Degrees): Future[CommandResult] = ???

  override def rotateLeft(degrees: Degrees): Future[CommandResult] = ???

  override def location: Future[Position] = ???

  override def flyTo(location: Position): Future[CommandResult] = ???

  override def flyBackwards(distance: Distance): Future[CommandResult] = ???

  override def flyForwards(distance: Distance): Future[CommandResult] = ???

  override def flyRight(distance: Distance): Future[CommandResult] = ???

  override def flyLeft(distance: Distance): Future[CommandResult] = ???

  override def getControlMode: Future[ControlMode] = ???

  override def setControlMode(controlMode: ControlMode): Unit = ???

  override def getSystemInformation: SystemInformation = ???

  override def setSystemInformation(systemInformation: SystemInformation): Unit = ???

  override def getParameterList: Future[List[(ParameterId, ParameterValue)]] = ???

  override def getParameter(id: ParameterId): Future[ParameterValue] = ???

  override def setParameter(id: ParameterId, value: ParameterValue): Unit = ???

  //TODO: Rename? //*
  override def setAttitude(attitude: Attitude, altitude: Altitude): Future[CommandResult] = ???

  override def setAttitude(attitude: Attitude, thrust: Thrust): Future[CommandResult] = Future {
    val msg = new msg_set_roll_pitch_yaw_thrust(systemID, 1)

    attitude match {
      case AttitudeDeg(r, p, y) =>
        msg.roll = Math.toRadians(r).toFloat
        msg.pitch = Math.toRadians(p).toFloat
        msg.yaw = Math.toRadians(y % 360).toFloat

        uart ! WriteMAVLink(msg)
        CommandSuccess()

      case AttitudeRad(r, p, y) =>
        msg.roll = r
        msg.pitch = p
        msg.yaw = y % (2 * Math.PI).toFloat

        uart ! WriteMAVLink(msg)
        CommandSuccess()

      case _ =>
        CommandFailure(new Exception("Unknown attitude parameter"))
    }
  }

  override def attitude: Future[Attitude] = Future {
    ???
  }

  override def altitude: Future[Altitude] = Future {
    ???
  }

  //Application controlled
  override def leaveControl(): Unit = {
    val msg = new msg_change_operator_control(systemID, 0)
    msg.control_request = 1

    uart ! WriteMAVLink(msg)
  }

  override def takeControl(): Unit = {
    uart ! SetPrimary(context.self)
    val msg = new msg_change_operator_control(systemID, 0)
    msg.control_request = 0

    uart ! WriteMAVLink(msg)
  }

  override def disarmMotors: Future[CommandResult] = Future {
    if (hasFlags(sysStatus, MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED)) {
      val msg = new msg_set_mode(systemID, 0)
      msg.base_mode = removeFlag(sysStatus, MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED)
      uart ! WriteMAVLink(msg)
      CommandSuccess()
    }
    else CommandFailure(new Exception(cannotMsg("disarm motors", sysStatus)))
  }

  override def armMotors: Future[CommandResult] = Future {
    if (!hasFlags(sysStatus, MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED)) {
      val msg = new msg_set_mode(systemID, 0)
      msg.base_mode = addFlag(sysStatus, MAV_MODE.MAV_MODE_STABILIZE_ARMED)
      uart ! WriteMAVLink(msg)
      CommandSuccess()
    }
    else CommandFailure(new Exception(cannotMsg("arm motors", sysStatus)))
  }

  override def calibrateSensors: Future[CommandResult] = Future {
    if (hasFlags(sysStatus, MAV_MODE.MAV_MODE_PREFLIGHT)) {
      val cmd = new msg_command_long(systemID, 0)
      cmd.command = MAV_CMD.MAV_CMD_PREFLIGHT_CALIBRATION
      cmd.param1 = 1
      cmd.param2 = 1
      cmd.param3 = 1
      cmd.param4 = 1
      cmd.param5 = 1
      cmd.param6 = 1

      uart ! WriteMAVLink(cmd)
      CommandSuccess()
    }
    else CommandFailure(new Exception(cannotMsg("calibrate sensors", sysStatus)))
  }

  override def getFlightMode: Future[FlightMode] = ???

  override def systemStatus: Option[SystemStatus] = sysStatus

}

object HeliosAPIDefault {
  def addFlag(currentStatus: Option[SystemStatus], flags: Int*): Int = {
    (getMode(currentStatus) /: flags)(_ | _)
  }

  def removeFlag(currentStatus: Option[SystemStatus], flags: Int*): Int = {
    if(hasFlags(currentStatus, flags: _*))
      (getMode(currentStatus) /: flags)(_ - _)
    else
      getMode(currentStatus)
  }

  def hasFlags(currentStatus: Option[SystemStatus], flags: Int*): Boolean = {
    val mode = getMode(currentStatus)
    flags.forall(flag => (flag & mode) == flag)
  }

  def getMode(currentStatus: Option[SystemStatus]): Int = {
    currentStatus.map(_.mode).getOrElse(0)
  }

  def getMode2(currentStatus: Option[SystemStatus]): Int = {
    (for (s <- currentStatus) yield s.mode)
      .getOrElse(0)
  }

  def cannotMsg(thing: String, currentStatus: Option[SystemStatus]): String = {
    s"Cannot $thing in mode: ${getMode(currentStatus)}"
  }
}