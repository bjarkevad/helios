package helios.util.gpio

import helios.util.nio.AsyncFileChannel
import helios.util.nio.FileOps._
import helios.util.nio.AsyncFileChannel.AsyncFileChannelOps
import helios.util.gpio.GPIOPin.GPIOPin

import java.nio.file.StandardOpenOption

import scala.concurrent.Await
import scala.util.Try
import scala.io.Source

object GPIOPin extends Enumeration {
  type GPIOPin = Value
  val P8_14, P8_15, P8_16, P8_17 = Value
}


case class GPIO(pin: GPIOPin, dir: String)

object GPIO {

  import GPIOPin._

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


  private def getFullDir(gpioPin: GPIOPin): String = gpioDir + gpioPrefix + toSwPin(gpioPin)

  private def getFullDir(gpio: GPIO): String = getFullDir(gpio.pin)

  private def getDirectionDir(gpioPin: GPIOPin): String = getFullDir(gpioPin) + "/direction"

  private def getDirectionDir(gpio: GPIO): String = getDirectionDir(gpio.pin)

  private def getValueDir(gpioPin: GPIOPin): String = getFullDir(gpioPin) + "/value"

  private def getValueDir(gpio: GPIO): String = getValueDir(gpio.pin)

  private def ex(gpioPin: GPIOPin, file: String): GPIO = {
    writeLines(file, toSwPin(gpioPin))

    GPIO(gpioPin, getFullDir(gpioPin))
  }

  def export(gpioPin: GPIOPin): Try[GPIO] = Try(ex(gpioPin, export))

  def export(gpio: GPIO): Try[GPIO] = export(gpio.pin)

  def unexport(gpioPin: GPIOPin): Try[GPIO] = Try(ex(gpioPin, unexport))

  def unexport(gpio: GPIO): Try[GPIO] = unexport(gpio.pin)

  def isExported(gpio: GPIO): Boolean = exists(gpio.dir)

  def setDirection(gpio: GPIO, input: Boolean): Boolean = {
    val dir = input match {
      case true => "in"
      case false => "out"
    }

    writeLines(getDirectionDir(gpio), dir).isSuccess
  }

  def getDirection(gpio: GPIO): Option[String] = {
    readLines(getDirectionDir(gpio)).
      map(_.find(_.nonEmpty)).
      toOption.flatten
  }

  def setValue(gpio: GPIO, value: Int): Boolean = {
    writeLines(getValueDir(gpio), value.toString).isSuccess
  }

  def getValue(gpio: GPIO): Try[String] = {
    readLines(getValueDir(gpio)) map (_ mkString(" "))
  }

//  private def SetEdge(gpio: GPIO, edge: String): Int = ???
//
//  private def Open(gpio: GPIO): Boolean = ???
//
//  private def Close(gpio: GPIO): Boolean = ???
}
