package helios.api


import helios.apimessages.CoreMessages.RegisterAPIClient
import scala.concurrent.{Await, Future}
import rx.Observable
import akka.actor._
import akka.pattern.ask


trait Location

trait Distance

trait SystemStatus

trait Altitude

trait Attitude

trait Degree

trait CommandResult

case class CommandSuccess() extends CommandResult
case class CommandFailure() extends CommandResult

trait MissionResult

trait Mission

//A collection of MissionItems
trait MissionItem

trait ParameterId

trait ParameterValue

trait SystemInformation

trait ControlMode

trait FlightMode

trait Thrust

case class ByThrust() extends ControlMode

case class ByAltitude() extends ControlMode

trait HeliosAPI {

  def ping(ms: Long): Unit

  def getFlightMode: Future[FlightMode] //System flightmode

  def setStartUpHandler(f: => Unit): Unit

  def setShutDownHandler(f: => Unit): Unit

  def setCriticalHandler(f: => Unit): Unit

  def setEmergencyHandler(f: => Unit): Unit

  def calibrateSensors: Future[CommandResult] //*

  def armMotors: Future[CommandResult]

  //*
  def disarmMotors: Future[CommandResult] //*

  def systemStatus: Future[SystemStatus]

  //* heartbeat
  def systemStatusStream: Observable[SystemStatus] //*

  def takeControl(): Unit

  //Application controlled
  def leaveControl(): Unit //Controlled by joystick or autopilot

  def altitude: Future[Altitude] //*
  //  def setAltitude(altitude: Altitude): Future[CommandResult] //setAltitude(1 meter) //* //Fixed heigth

  def attitude: Future[Attitude]

  //*
  def setAttitude(attitude: Attitude, thrust: Thrust): Future[CommandResult]

  //TODO: Rename? //*
  def setAttitude(attitude: Attitude, altitude: Altitude): Future[CommandResult] //TODO: Rename? //*

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

class HeliosApplication extends Actor {
  lazy val Helios: HeliosAPI = {
    import scala.concurrent.duration._

    println("Started API App")
    val clientRecep =
      context.actorSelection("akka.tcp://Main@localhost:2552/user/app")

    val f = ask(clientRecep, RegisterAPIClient(self))(3 seconds).mapTo[HeliosAPI]

    Await.result(f, 4 seconds)
  }

  override def preStart() = {

  }

  override def receive: Actor.Receive = {
    case _ =>
  }
}
