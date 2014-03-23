package helios.api

import scala.concurrent.Future
import rx.lang.scala.Observable

object HeliosAPI {


  trait Location

  case class SystemLocation(location: Location)

  trait Distance

  case class SystemStatus(mavtype: Int, autopilot: Int, mode: Int, status: Int, seq: Int = -1)

  trait Altitude

  trait Attitude

  case class AttitudeDeg(roll: Degrees, pitch: Degrees, yaw: Degrees) extends Attitude

  case class AttitudeRad(roll: Radians, pitch: Radians, yaw: Radians) extends Attitude

  type Degrees = Float
  type Radians = Float

  trait CommandResult

  case class CommandSuccess() extends CommandResult

  case class CommandFailure(reason: Throwable) extends CommandResult

  trait MissionResult

  trait Mission

  trait MissionItem

  trait ParameterId

  trait ParameterValue

  trait SystemInformation

  trait FlightMode

  type Thrust = Float

  trait ControlMode

  case class ByThrust() extends ControlMode

  case class ByAltitude() extends ControlMode

}

trait HeliosAPI {

  import HeliosAPI._

  //Returns the difference between sent_ms and received_ms
  def ping(sent_ms: Long): Future[Long]

  //PUBLIC
  def terminate(): Unit

  def getFlightMode: Future[FlightMode] //System flightmode

//  def setStartUpHandler(f: () => Unit): Unit
//
//  def setShutDownHandler(f: () => Unit): Unit

  def calibrateSensors: Future[CommandResult]

  def armMotors: Future[CommandResult]

  def disarmMotors: Future[CommandResult]

  def systemStatus: Option[SystemStatus]

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

  def rotateLeft(degrees: Degrees): Future[CommandResult]

  def rotateRight(degrees: Degrees): Future[CommandResult]

  def land: Future[CommandResult]

  def takeOff: Future[CommandResult]

  def startMission(): Observable[MissionResult]

  def newMission(mission: Mission): Unit
}

//trait HeliosPrivate {
//}


