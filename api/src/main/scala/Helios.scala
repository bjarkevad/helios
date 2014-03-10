package helios.api


import helios.apimessages.CoreMessages.RegisterAPIClient
import scala.concurrent.{Await, Future}
import rx.Observable
import akka.actor._
import akka.pattern.ask
import org.mavlink.messages.MAV_STATE


object HeliosAPI {


  trait Location
  trait Distance
  case class SystemStatus(mavtype: Int, autopilot: Int, state: Int, seq: Int = -1)
  trait Altitude
  trait Attitude
  trait Degree
  trait CommandResult

  case class CommandSuccess() extends CommandResult
  case class CommandFailure() extends CommandResult

  trait MissionResult
  trait Mission
  trait MissionItem
  trait ParameterId
  trait ParameterValue
  trait SystemInformation
  trait FlightMode
  trait Thrust
  trait ControlMode

  case class ByThrust() extends ControlMode
  case class ByAltitude() extends ControlMode

}

trait HeliosAPI {

  import HeliosAPI._

  //PRIVATE
  def updateSystemStatus(status: SystemStatus): Unit

  def ping(ms: Long): Unit

  def getFlightMode: Future[FlightMode] //System flightmode

  def setStartUpHandler(f: => Unit): Unit

  def setShutDownHandler(f: => Unit): Unit

  def setCriticalHandler(f: => Unit): Unit

  def setEmergencyHandler(f: => Unit): Unit

  def calibrateSensors: Future[CommandResult]

  def armMotors: Future[CommandResult]

  def disarmMotors: Future[CommandResult]

  def systemStatus: Option[SystemStatus]

  //* heartbeat
  def systemStatusStream: Observable[SystemStatus]

  def takeControl(): Unit

  def leaveControl(): Unit

  def altitude: Future[Altitude]
  //  def setAltitude(altitude: Altitude): Future[CommandResult] //setAltitude(1 meter) //* //Fixed heigth

  def attitude: Future[Attitude]

  def setAttitude(attitude: Attitude, thrust: Thrust): Future[CommandResult]

  def setAttitude(attitude: Attitude, altitude: Altitude): Future[CommandResult]

  def setParameter(id: ParameterId, value: ParameterValue): Unit

  def getParameter(id: ParameterId): Future[ParameterValue]

  def getParameterList: Future[List[(ParameterId, ParameterValue)]]

  def setSystemInformation(systemInformation: SystemInformation): Unit

  def getSystemInformation: SystemInformation

  def setControlMode(controlMode: ControlMode): Unit

  def getControlMode: Future[ControlMode]

  def flyLeft(distance: Distance): Future[CommandResult]

  def flyRight(distance: Distance): Future[CommandResult]

  def flyForwards(distance: Distance): Future[CommandResult]

  def flyBackwards(distance: Distance): Future[CommandResult]

  def flyTo(location: Location): Future[CommandResult]

  def location: Future[Location]

  def locationStream: Observable[Location]

  def rotateLeft(degrees: Degree): Future[CommandResult]

  def rotateRight(degrees: Degree): Future[CommandResult]

  def land: Future[CommandResult]

  def takeOff: Future[CommandResult]

  def startMission(): Observable[MissionResult]

  def newMission(mission: Mission): Unit
}

trait HeliosPrivate {
  import HeliosAPI._

}


