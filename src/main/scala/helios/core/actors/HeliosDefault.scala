package helios.core.actors

import akka.actor._
import akka.pattern.ask
import helios.apimessages.CoreMessages._
import rx.Observable
import scala.concurrent.{Promise, Future}
import scala.util.Try

import java.lang.System.currentTimeMillis
import org.slf4j.LoggerFactory
import helios.apimessages.MAVLinkMessages.RawMAVLink
import helios.api.{HeliosPrivate, HeliosAPI}
import helios.api.HeliosAPI._

class HeliosDefault(val name: String, val clientReceptionist: ActorRef, val client: ActorRef) extends HeliosAPI
with TypedActor.PreStart
with TypedActor.PostStop
with TypedActor.Receiver {

  lazy val context: ActorContext = TypedActor.context
  lazy val logger = LoggerFactory.getLogger(classOf[HeliosDefault])

  var sysStatus: Option[SystemStatus] = None

  override def preStart() = {
    //context.parent ! RegisterClient(context.self)
  }

  override def postStop() = {
    //clientReceptionist ! PoisonPill
  }

  override def updateSystemStatus(status: SystemStatus): Unit = {
    logger.debug("updateSystemStatus")
    sysStatus = Some(status)
  }

  override def onReceive(msg: Any, sender: ActorRef) = {
    msg match {
      case m@RawMAVLink(_) => client ! m
      case _ => logger.warn("API received something unknown")
    }
  }

  override def ping(ms: Long): Unit = {
    println(currentTimeMillis() - ms)
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

  override def locationStream: Observable[Location] = ???

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


  override def disarmMotors: Future[CommandResult] = ???

  override def armMotors: Future[CommandResult] = ???

  override def calibrateSensors: Future[CommandResult] = ???

  override def setEmergencyHandler(f: => Unit): Unit = ???

  override def setCriticalHandler(f: => Unit): Unit = ???

  override def setShutDownHandler(f: => Unit): Unit = ???

  override def setStartUpHandler(f: => Unit): Unit = ???

  override def getFlightMode: Future[FlightMode] = ???

  override def systemStatus: Option[SystemStatus] = {
    sysStatus
  }
    //clientReceptionist ?
  override def systemStatusStream: Observable[SystemStatus] = ???
}

