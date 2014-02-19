package helios.util.gpio

import helios.util.nio.AsyncFileChannel
import helios.util.nio.FileOps._
import helios.util.nio.AsyncFileChannel.AsyncFileChannelOps
import helios.util.gpio.GPIOPin.GPIOPin

import java.nio.file.StandardOpenOption

import scala.concurrent.Await

object GPIOPin extends Enumeration {
  type GPIOPin = Value
  val P8_14, P8_15, P8_16, P8_17 = Value
}


case class GPIO(pin: GPIOPin, dir: String)

object GPIO {
  import GPIOPin._
  import scala.concurrent.duration._
  import scala.language.postfixOps

  def toSwPin(gpioPin: GPIOPin) = {
     gpioPin match {
      case P8_14 => "10"
      case P8_15 => "15"
      case P8_16 => "14"
      case P8_17 => "11"
      case _ => ""
    }
  }

  private val gpioDir = "/sys/class/gpio/"
  private val export = gpioDir + "export"
  private val unexport = gpioDir + "unexport"
  private val gpioPrefix = "gpio"

  private def ex(gpioPin: GPIOPin, file: String) = {
    val swPin = toSwPin(gpioPin)
    val asyncFC = AsyncFileChannel(file, Set(StandardOpenOption.WRITE))
    asyncFC.map(a => Await.result(a.writeAsync(swPin), 5 millis)) //TODO: Blocking or not?
    GPIO(gpioPin, toFullDir(gpioPin))
  }

  private def toFullDir(gpioPin: GPIOPin) = {
    gpioDir + gpioPrefix + toSwPin(gpioPin)
  }

  def Export(gpioPin: GPIOPin): GPIO = ex(gpioPin, export)

  def Unexport(gpioPin: GPIOPin): GPIO = ex(gpioPin, unexport)

  def IsExported(gpio: GPIO): Boolean = exists(gpio.dir).getOrElse(false)

  def SetDirection(gpio: GPIO, input: Boolean): Boolean = ???

  def SetValue(gpio: GPIO, value: Int): Boolean = ???

  def GetValue(gpio: GPIO, value: Int): Int = ???

  def SetEdge(gpio: GPIO, edge: String): Int = ???

  def Open(gpio: GPIO): Boolean = ???

  def Close(gpio: GPIO): Boolean = ???
}
