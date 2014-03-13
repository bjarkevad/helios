package helios.core.actors

import akka.actor._
import scala.concurrent.{Promise, Future}
import scala.util.Try
import org.slf4j.LoggerFactory
import helios.apimessages.CoreMessages._
import helios.apimessages.MAVLinkMessages.RawMAVLink
import helios.api.HeliosAPI
import helios.api.HeliosAPI._
import helios.core.actors.ClientReceptionist.PublishMAVLink
import org.mavlink.messages._
import org.mavlink.messages.common._
import rx.lang.scala.Observable

class HeliosAPIDefault(val name: String, val clientReceptionist: ActorRef, val client: ActorRef) extends HeliosAPI
with TypedActor.PreStart
with TypedActor.PostStop
with TypedActor.Receiver {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val context: ActorContext = TypedActor.context
  lazy val logger = LoggerFactory.getLogger(classOf[HeliosAPIDefault])

  var criticalHandler: () => Unit =
    () => logger.warn("System entered critical mode with no handler!")
  var emergencyHandler: () => Unit =
    () => logger.warn("System entered emergency mode with no handler!")

  override def setCriticalHandler(f: () => Unit): Unit = criticalHandler = f

  override def setEmergencyHandler(f: () => Unit): Unit = emergencyHandler = f

  //TODO: Move to client side
  var sysStatus: Option[SystemStatus] = None

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

  override def updateSystemStatus(status: SystemStatus): Unit = {
    logger.debug("updateSystemStatus")
    sysStatus = Some(status)
    client ! status
  }

  override def onReceive(msg: Any, sender: ActorRef) = {
    msg match {
      case m@PublishMAVLink(ml) =>
        if (ml.messageType == 0) {
          val hb = ml.asInstanceOf[msg_heartbeat]
          updateSystemStatus(SystemStatus(hb.`type`, hb.autopilot, hb.base_mode, hb.system_status, hb.sequence))
        }

      case Terminated(`client`) =>
        logger.warn("Client was terminated, killing self")
        context.self ! PoisonPill

      case _ =>
        logger.warn("API received something unknown")
    }
  }

  override def ping(ms: Long): Unit = {
    //println(currentTimeMillis() - ms)
  }

  override def newMission(mission: Mission): Unit = ???

  override def startMission(): Observable[MissionResult] = ???

  override def takeOff: Future[CommandResult] = {
    clientReceptionist ! TestMsg

    val p = Promise[CommandResult]

    p.complete(Try(new CommandSuccess))

    p.future
  }

  override def land: Future[CommandResult] = ???

  override def rotateRight(degrees: Degree): Future[CommandResult] = ???

  override def rotateLeft(degrees: Degree): Future[CommandResult] = ???

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

  override def setAttitude(attitude: Attitude, thrust: Thrust): Future[CommandResult] = ???

  override def attitude: Future[Attitude] = ???

  override def altitude: Future[Altitude] = ???

  //Application controlled
  override def leaveControl(): Unit = ???

  override def takeControl(): Unit = ???


  override def disarmMotors: Future[CommandResult] = {
    Future {
      val msg = new msg_set_mode
      msg.target_system = 20
      msg.base_mode = MAV_MODE.MAV_MODE_STABILIZE_DISARMED

      clientReceptionist ! RawMAVLink(msg)
      CommandSuccess()
    }
  }

  override def armMotors: Future[CommandResult] = {
    Future {
      val msg = new msg_set_mode
      msg.target_system = 20
      msg.base_mode = MAV_MODE.MAV_MODE_STABILIZE_ARMED
      //createBasemode(systemStatus, MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED)

      clientReceptionist ! RawMAVLink(msg)
      CommandSuccess()
    }
  }

  override def calibrateSensors: Future[CommandResult] = {
    Future {
      sysStatus match {
        //can only calibrate in preflight mode
        case Some(s) if s.mode == MAV_MODE.MAV_MODE_PREFLIGHT =>
          val cmd = new msg_command_long
          cmd.target_system = 20 //TODO: Configure system id
          cmd.target_component = 0 //TODO: Figure out this number
          cmd.command = MAV_CMD.MAV_CMD_PREFLIGHT_CALIBRATION
          cmd.param1 = 1
          cmd.param2 = 1
          cmd.param3 = 1
          cmd.param4 = 1
          cmd.param5 = 1
          cmd.param6 = 1

          clientReceptionist ! RawMAVLink(cmd)
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

  //override def systemStatusStream: Observable[SystemStatus] = sysSubject
}

object HeliosAPIDefault {
  def createBasemode(currentStatus: Option[SystemStatus], flags: Int*): Int = {
    ((currentStatus map (_.mode)).getOrElse(0)
      /: flags)(_ | _)
  }
}

