package helios

import helios.api.{HeliosLocal, HeliosRemote, HeliosAPI}
import scala.language.postfixOps
import helios.api.HeliosAPI._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.util.ByteString
import helios.api.Streams._
import helios.api.HeliosAPI.AttitudeDeg
import helios.Spektrum.Channels
import scala.collection.mutable

object Joystick {

  import helios.Spektrum.channelsImpls
  import helios.Spektrum

  def joystickHandler(input: Byte)(implicit maxDeg: Int = 25): Option[(AttitudeDeg, Thrust)] = {
    val factor = maxDeg / 1000

    def attitude(channels: Channels[Int]): AttitudeDeg = {
      AttitudeDeg(
        channels.roll * factor,
        channels.pitch * factor,
        channels.yaw * factor
      )
    }

    def thrust(channels: Channels[Int]): Thrust =
      channels.throttle.toFloat * factor

    Spektrum.parse(input.toInt) match {
      case Some(channels) =>
        Some(attitude(channels), thrust(channels))
      case None => None
    }
  }

  val HeliosApp = HeliosLocal()
  val Helios = HeliosApp.Helios

  Helios.uartStream.subscribe {
    _.foreach {
      b =>
        joystickHandler(b) match {
          case Some((attitude, thrust)) =>
            Helios.setAttitude(attitude, thrust)
          case None =>
        }
    }
  }
}

object Spektrum {
  type Channels[T] = mutable.Buffer[T]

  val buffer: mutable.Buffer[Int] = mutable.Buffer.empty
  val pkgsize = 16

  implicit class channelsImpls[T](channels: Channels[T]) {
    def roll = channels(0)

    def trainer = channels(1)

    def pitch = channels(2)

    def click = channels(3)

    def yaw = channels(4)

    def throttle = channels(5)
  }

  def bufferIsFull: Boolean = buffer.size >= pkgsize

  def parse(c: Int): Option[Channels[Int]] = {
    if (bufferIsFull)
      Some(channels(buffer))
    else {
      buffer append c
      None
    }
  }

  def channels(buffer: mutable.Buffer[Int]): Channels[Int] = {
    assert(bufferIsFull)
    val channels: mutable.Buffer[Int] = mutable.Buffer.empty

    channels append ((buffer(2) << 8) | buffer(3))
    channels append ((buffer(4) << 8) | buffer(5))
    channels append ((buffer(6) << 8) | buffer(7))
    channels append ((buffer(8) << 8) | buffer(9))
    channels append ((buffer(10) << 8) | buffer(11))
    channels append ((buffer(14) << 8) | buffer(15))

    buffer.clear()

    channels
  }
}
