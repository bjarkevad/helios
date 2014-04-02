package helios

import helios.api.HeliosApplication
import helios.api.Streams._
import helios.api.Handlers._
import helios.api.HeliosAPI.{AttitudeRad, SystemPosition}
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object Waypoints extends App {

  val dt = 1000
  val pidYaw = new PID(0.8, 0.1, 0, dt)
  //  val pidPitch = new PID(1, 0, 0, dt)

  def angleBetween(first: (Long, Long), second: (Long, Long)): Float = {
    if (first != second) {
      val (x1, y1) = first
      val (x2, y2) = second

      val opp = y2 - y1
      val hyp = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(opp, 2))

      Math.asin(opp / hyp).toFloat
    } else 0
  }

  //TODO: VAAAR
  var previousDir: (Long, Long) = (0, 0)

  def calcYaw(position: SystemPosition, setpoint: SystemPosition): Future[Float] = Future {
    val actualVec = (position.lat - previousDir._1, position.lon - previousDir._2)
    val setpointVec = (setpoint.lat - previousDir._1, setpoint.lon - previousDir._2)

    val newDir = angleBetween(actualVec, setpointVec)
    val actualDir = angleBetween(actualVec, (0, 0))

    previousDir = actualVec

    val o = pidYaw.nextOutput(newDir, actualDir).toFloat
    println(s"New yaw: $o")
    o
  }

  def calcPitch(position: SystemPosition, setpoint: SystemPosition): Future[Float] = Future {
    val dlat = setpoint.lat - position.lat
    val dlon = setpoint.lon - position.lon
    val distance = Math.sqrt(Math.pow(dlat, 2) + Math.pow(dlon, 2)).toFloat

    val pitch = distance * 0.01f

    if (Math.abs(pitch) >= 10) 10
    else if (Math.abs(pitch) <= 5) 0
    else pitch
  }

  def calcAttitude(position: SystemPosition)(implicit setpoint: SystemPosition): Future[AttitudeRad] = {
    calcPitch(position, setpoint)
      .zip(calcYaw(position, setpoint))
      .map {
      case (p, y) => AttitudeRad(0, p, y)
    }
  }

  implicit val setpoint: SystemPosition = SystemPosition(50,15,3,0,0,0,0)

  val helios = HeliosApplication().Helios

  helios.takeControl()
  Await.result(helios.takeOff, 10 seconds)

  val flyTask = helios.positionStream
    .map(calcAttitude)
    .subscribe(_.map(helios.setAttitude(_, 0.5f)))

  helios.setEmergencyHandler(() => flyTask.unsubscribe())
}
