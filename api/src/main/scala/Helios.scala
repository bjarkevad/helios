package helios.api

import scala.concurrent.Future
import rx.Observable


trait Location
trait Distance
trait SystemStatus
trait Altitude
trait Attitude
trait Degree
trait CommandResult
trait MissionResult
trait Mission //A collection of mission items
trait ParameterId
trait ParameterValue
trait SystemInformation

trait HeliosAPI  {
  def setSystemInformation(systemInformation: SystemInformation): Unit
  def getSystemInformation: SystemInformation

  def setStartUpHandler(f: => Unit): Unit
  def setShutDownHandler(f: => Unit): Unit

  def setCriticalHandler(f: => Unit): Unit
  def setEmergencyHandler(f: => Unit): Unit

  def armMotors: Future[CommandResult]
  def disarmMotors: Future[CommandResult]

  def land: Future[CommandResult]
  def takeOff: Future[CommandResult]

  def flyTo(location: Location): Future[CommandResult]
  def location: Future[Location]
  def locationStream: Observable[Location]

  def systemStatus: Future[SystemStatus]
  def systemStatusStream: Observable[SystemStatus]

  def altitude: Future[Altitude]
  def setAltitude(altitude: Altitude): Future[CommandResult] //setAltitude(1 meter)

  def attitude: Future[Attitude]
  def setAttitude(attitude: Attitude): Future[CommandResult] //TODO: Rename?

  def flyLeft(distance: Distance): Future[CommandResult]
  def flyRight(distance: Distance): Future[CommandResult]
  def flyForwards(distance: Distance): Future[CommandResult]
  def flyBackwards(distance: Distance): Future[CommandResult]

  def rotateLeft(degrees: Degree): Future[CommandResult]
  def rotateRight(degrees: Degree): Future[CommandResult]

  def setParameter(id: ParameterId, value: ParameterValue): Unit
  def getParameter(id: ParameterId): Future[ParameterValue]

  def startMission(): Observable[MissionResult]
  def newMission(mission: Mission): Unit
}

//class HeliosDefault(val name: String) extends HeliosAPI {
//  def this() = this("Helios")
//}
