package helios.core.actors

import akka.actor._
import scala.concurrent.Future
import org.slf4j.LoggerFactory
import helios.api.HeliosAPI
import helios.api.HeliosAPI._
import helios.api.HeliosApplicationDefault._
import helios.api.messages.MAVLinkMessages.PublishMAVLink
import helios.core.actors.flightcontroller.FlightControllerMessages.WriteMAVLink
import org.mavlink.messages._
import org.mavlink.messages.common._
import rx.lang.scala.Observable
import helios.core.actors.flightcontroller.HeliosUART.SetPrimary
import helios.core.actors.flightcontroller.HeliosUART
import java.lang.System.currentTimeMillis

class HeliosAPIDefault(val name: String, val clientReceptionist: ActorRef, val client: ActorRef, val uart: ActorRef, val systemID: Int) extends HeliosAPI
with TypedActor.PreStart
with TypedActor.PostStop
with TypedActor.Receiver {

  import scala.concurrent.ExecutionContext.Implicits.global

  val context: ActorContext = TypedActor.context
  implicit val sender: ActorRef = context.self

  lazy val logger = LoggerFactory.getLogger(classOf[HeliosAPIDefault])

  var criticalHandler: () => Unit =
    () => logger.warn("System entered critical mode with no handler!")
  var emergencyHandler: () => Unit =
    () => logger.warn("System entered emergency mode with no handler!")

  override def setCriticalHandler(f: () => Unit): Unit =
    criticalHandler = f

  override def setEmergencyHandler(f: () => Unit): Unit =
    emergencyHandler = f

  //TODO: Move to client side
  var sysStatus: Option[SystemStatus] = None
  var sysLocation: Option[SystemLocation] = None
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

  def updateSystemLocation(location: SystemLocation): Unit = {
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

          case _ =>
        }

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

  override def newMission(mission: Mission): Unit = ???

  override def startMission(): Observable[MissionResult] = ???

  override def takeOff: Future[CommandResult] = {
    Future {
      val msg = new msg_command_long(systemID, 0)
      msg.command = MAV_CMD.MAV_CMD_NAV_TAKEOFF
      msg.param1 = 0 //pitch
      msg.param4 = 0 //yaw
      msg.param5 = 0 //latitude
      msg.param6 = 0 //longtitude
      msg.param7 = 2 //altitude

      CommandSuccess()
    }
  }

  override def land: Future[CommandResult] = {
    Future {
      val msg = new msg_command_long(systemID, 0)
      msg.command = MAV_CMD.MAV_CMD_NAV_LAND
      msg.param4 = 0 //desired yaw
      msg.param5 = 0 //latitude
      msg.param6 = 0 //longtitude
      msg.param7 = 0 //altitude

      uart ! WriteMAVLink(msg)

      CommandSuccess()
    }
  }

  override def rotateRight(degrees: Degrees): Future[CommandResult] = ???

  override def rotateLeft(degrees: Degrees): Future[CommandResult] = ???

  override def location: Future[Location] = ???

  override def flyTo(location: Location): Future[CommandResult] = ???

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

  override def setAttitude(attitude: Attitude, thrust: Thrust): Future[CommandResult] = {
    Future {
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
  }

  override def attitude: Future[Attitude] = {
    ???
  }

  override def altitude: Future[Altitude] = {
    ???
  }

  //Application controlled
  override def leaveControl(): Unit = ???

  override def takeControl(): Unit = {
    uart ! SetPrimary(context.self)
  }

  override def disarmMotors: Future[CommandResult] = {
    Future {
      val msg = new msg_set_mode(systemID, 0)
      //msg.target_system = 20
      msg.base_mode = MAV_MODE.MAV_MODE_STABILIZE_DISARMED

      uart ! WriteMAVLink(msg)
      CommandSuccess()
    }
  }

  override def armMotors: Future[CommandResult] = {
    Future {
      val msg = new msg_set_mode(systemID, 0)
      //msg.target_system = 20
      msg.base_mode = MAV_MODE.MAV_MODE_STABILIZE_ARMED
      //createBasemode(systemStatus, MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED)

      uart ! WriteMAVLink(msg)
      CommandSuccess()
    }
  }

  override def calibrateSensors: Future[CommandResult] = {
    Future {
      sysStatus match {
        //can only calibrate in preflight mode
        case Some(s) if s.mode == MAV_MODE.MAV_MODE_PREFLIGHT =>
          val cmd = new msg_command_long(systemID, 0)
          //cmd.target_system = 20 //TODO: Configure system id
          //cmd.target_component = 0 //TODO: Figure out this number
          cmd.command = MAV_CMD.MAV_CMD_PREFLIGHT_CALIBRATION
          cmd.param1 = 1
          cmd.param2 = 1
          cmd.param3 = 1
          cmd.param4 = 1
          cmd.param5 = 1
          cmd.param6 = 1

          uart ! WriteMAVLink(cmd)
          CommandSuccess()

        case Some(s) =>
          CommandFailure(new Exception(s"Not allowed in current mode: ${s.mode}"))

        case None =>
          CommandFailure(new Exception("Could not determine system status"))
      }
    }
  }

  override def setShutDownHandler(f: () => Unit): Unit = ???

  override def setStartUpHandler(f: () => Unit): Unit = ???

  override def getFlightMode: Future[FlightMode] = ???

  override def systemStatus: Option[SystemStatus] = sysStatus

}

object HeliosAPIDefault {
  def createBasemode(currentStatus: Option[SystemStatus], flags: Int*): Int = {
    (currentStatus.map(_.mode).getOrElse(0)
      /: flags)(_ | _)
  }
}

