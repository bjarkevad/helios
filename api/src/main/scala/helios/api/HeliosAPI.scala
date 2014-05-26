package helios.api

import scala.concurrent.Future
import rx.lang.scala.Observable
import akka.util.ByteString

object HeliosAPI {


  trait Position

  case class SystemPosition(lat: Long, lon: Long, alt: Long, relAlt: Long,
                            vx: Int, vy: Int, vz: Int,
                            hdg: Int = Int.MaxValue) extends Position

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

  type Meters = Float

}

trait HeliosAPI {

  import HeliosAPI._

  //Returns the difference between sent_ms and received_ms
  def ping(sent_ms: Long): Future[Long]

  //PUBLIC
  def terminate(): Unit

  def writeToUart(data: String): Unit

//  def setStartUpHandler(f: () => Unit): Unit
//
//  def setShutDownHandler(f: () => Unit): Unit

  def systemStatus: Option[SystemStatus]

  def calibrateSensors: Future[CommandResult]

  def armMotors: Future[CommandResult]

  def disarmMotors: Future[CommandResult]

  def takeControl(): Unit

  def leaveControl(): Unit

  //  def setAltitude(altitude: Altitude): Future[CommandResult] //setAltitude(1 meter) //* //Fixed heigth

  def setAttitude(attitude: Attitude, thrust: Thrust): Future[CommandResult]

  def setAttitude(attitude: Attitude, altitude: Altitude): Future[CommandResult]

  def setParameter(id: ParameterId, value: ParameterValue): Unit

  def getParameter(id: ParameterId): Future[ParameterValue]

  def getParameterList: Future[List[(ParameterId, ParameterValue)]]

  def flyTo(location: Position): Future[CommandResult]

  def land: Future[CommandResult]

  def takeOff(height: Meters): Future[CommandResult]

//  def startMission(): Observable[MissionResult]
//
//  def newMission(mission: Mission): Unit
}

//trait HeliosPrivate {
//}


