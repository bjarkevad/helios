package helios.api

import akka.actor.Scheduler
import rx.lang.scala.{Observable, Subject}
import helios.api.HeliosAPI._
import akka.util.ByteString
import org.mavlink.messages.MAVLinkMessage


trait HeliosApplication {
  val Helios: HeliosAPI
  val scheduler: Scheduler
}

//TODO: Use BehaviorSubjects to cache the latest message for new subscribers
object Streams {
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

  implicit class StreamsImpl(val helios: HeliosAPI) {
    lazy val systemStatusStream: Observable[SystemStatus] = statusStream
    lazy val positionStream: Observable[SystemPosition] = posStream
    lazy val attitudeRadStream: Observable[AttitudeRad] = attStream
    lazy val attitudeDegStream: Observable[AttitudeDeg] = attStream map {
      a =>
        AttitudeDeg(
          Math.toDegrees(a.roll).toFloat,
          Math.toDegrees(a.pitch).toFloat,
          Math.toDegrees(a.yaw).toFloat
        )
    }

    lazy val uartStream: Observable[ByteString] = uStream
    lazy val groundControlMAVLinkStream: Observable[MAVLinkMessage] = gcMlStream
    lazy val flightcontrollerMAVLinkStream: Observable[MAVLinkMessage] = fcMlStream
  }
}

object Handlers {
  private[api]
  var criticalHandler: () => Unit =
    () => println("System entered critical mode with no handler!")

  var emergencyHandler: () => Unit =
    () => println("System entered emergency mode with no handler!")

  implicit class HandlersImp(val helios: HeliosAPI) {
    def setCriticalHandler(f: () => Unit): Unit =
      criticalHandler = f

    def setEmergencyHandler(f: () => Unit): Unit =
      emergencyHandler = f
  }
}

