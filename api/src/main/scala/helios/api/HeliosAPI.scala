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

  /**
   * pings the underlying system
   * @param sent_ms usually 'now'
   * @return the difference between sent_ms and the time it was received by the underlying system
   */
  def ping(sent_ms: Long): Future[Long]

  /**
   * Terminates the system
   */
  def terminate(): Unit

  /**
   * Write data to UARTs in the system
   * @param data the data to Write
   */
  def writeToUart(data: String): Unit

  /**
   * Gets the current system status
   * @return Some containing the status of the system, None if the system status is not available
   */
  def systemStatus: Option[SystemStatus]

  /**
   * Calibrate the sensors on the Flight Controller
   * @return A Future containing CommandSuccess if the Flight Controller was correctly given the command, CommandFailure otherwise
   */
  def calibrateSensors: Future[CommandResult]

  /**
   * Arms the motors
   * @return A Future containing CommandSuccess if motors were armed succesfully, CommandFailure otherwise
   */
  def armMotors: Future[CommandResult]

  /**
   * Disarms the motors
   * @return A Future containing CommandSuccess if motors were disarmed succesfully, CommandFailure otherwise
   */
  def disarmMotors: Future[CommandResult]

  /**
   * Take control of the system. This function needs to be called before access to flight commands is given
   */
  def takeControl(): Unit

  /**
   * Leaves control of the system, inverts the effect of takeControl()
   */
  def leaveControl(): Unit

  /**
   * Sets the attitude of the drone
   * @param attitude The attitude (pitch, yaw, roll) of the drone
   * @param thrust The thrust put on the motors
   * @return A Future containing CommandSuccess if the command was succesful
   */
  def setAttitude(attitude: Attitude, thrust: Thrust): Future[CommandResult]

  /**
   * Sets the attitude of the drone
   * @param attitude The attitude (pitch, yaw, roll) of the drone
   * @param altitude The target altitude
   * @return A Future containing CommandSuccess if the command was succesful
   */
  def setAttitude(attitude: Attitude, altitude: Altitude): Future[CommandResult]

  /**
   * Sets a parameter on the system
   * @param id the ID of the parameter to set
   * @param value the value of the parameter
   */
  def setParameter(id: ParameterId, value: ParameterValue): Unit

  /**
   * Gets the value of a parameter on the system
   * @param id The ID of the value to get
   * @return The value of the parameter
   */
  def getParameter(id: ParameterId): Future[ParameterValue]

  /**
   * Gets the values and IDs of all parameters in the system
   * @return A list containing all IDs and values of parameters in the system
   */
  def getParameterList: Future[List[(ParameterId, ParameterValue)]]

  /**
   * fly to a certain position
   * @param location the position to fly to
   * @return A Future containing CommandSuccess if the given command was successful
   */
  def flyTo(location: Position): Future[CommandResult]

  /**
   * Intructs the drone to land
   * @return A Future containing CommandSuccess if the given command was successful
   */
  def land: Future[CommandResult]

  /**
   * Instruct the drone to take off
   * @param height The height to which the drone should fly
   * @return A Future containing CommandSuccess if the command was successful
   */
  def takeOff(height: Meters): Future[CommandResult]

  //  def setStartUpHandler(f: () => Unit): Unit
  //
  //  def setShutDownHandler(f: () => Unit): Unit
  //  def setAltitude(altitude: Altitude): Future[CommandResult] //setAltitude(1 meter) //* //Fixed health
  //  def startMission(): Observable[MissionResult]
  //
  //  def newMission(mission: Mission): Unit
}



