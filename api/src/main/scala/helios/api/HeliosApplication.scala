package helios.api

import akka.actor.Scheduler
import rx.lang.scala.{Observable, Subject}
import helios.api.HeliosAPI._
import akka.util.ByteString
import org.mavlink.messages.MAVLinkMessage


/**
 * Trait used to implement the top level of the API
 */
trait HeliosApplication {
  val Streams: Streams = new Streams()
  val Helios: HeliosAPI
  val scheduler: Scheduler
}

//TODO: Use BehaviorSubjects to cache the latest message for new subscribers
class Streams {
  private[api]
  lazy val statusStream: Subject[SystemStatus] = Subject()

  private[api]
  lazy val posStream: Subject[SystemPosition] = Subject()

  private[api]
  lazy val attStream: Subject[AttitudeRad] = Subject()

  private [api]
  lazy val uStream: Subject[ByteString] = Subject()

  private [api]
  lazy val gcMlStream: Subject[MAVLinkMessage] = Subject()

  private [api]
  lazy val fcMlStream: Subject[MAVLinkMessage] = Subject()

  /**
   * A stream of system status
   */
  lazy val systemStatusStream: Observable[SystemStatus] = statusStream
  /**
   * A stream of system positions
   */
  lazy val positionStream: Observable[SystemPosition] = posStream
  /**
   * A stream from uarts on the system
   */
  lazy val uartStream: Observable[ByteString] = uStream
  /**
   * A stream of MAVLink Messages sent from ground stations
   */
  lazy val groundControlMAVLinkStream: Observable[MAVLinkMessage] = gcMlStream
  /**
   * A stream of MAVLink messages sent from the flight controller
   */
  lazy val flightcontrollerMAVLinkStream: Observable[MAVLinkMessage] = fcMlStream
  /**
   * A stream of all measurede attitudes in radians
   */
  lazy val attitudeRadStream: Observable[AttitudeRad] = attStream
  /**
   * A stream of all measurede attitudes in degrees
   */
  lazy val attitudeDegStream: Observable[AttitudeDeg] = attStream map {
    a =>
      AttitudeDeg(
        Math.toDegrees(a.roll).toFloat,
        Math.toDegrees(a.pitch).toFloat,
        Math.toDegrees(a.yaw).toFloat
      )
  }

  lazy val attitudeCommandStream: Observable[AttitudeRad] = ???
}

object Handlers {
  private[api]
  var criticalHandler: () => Unit =
    () => println("System entered critical mode with no handler!")
    
  private[api]
  var emergencyHandler: () => Unit =
    () => println("System entered emergency mode with no handler!")

  implicit class HandlersImp(val helios: HeliosAPI) {
    def setCriticalHandler(f: () => Unit): Unit =
      criticalHandler = f

    def setEmergencyHandler(f: () => Unit): Unit =
      emergencyHandler = f
  }
}