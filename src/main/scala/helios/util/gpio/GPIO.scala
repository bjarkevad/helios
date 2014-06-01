package helios.util.gpio

import helios.util.nio.AsyncFileChannel
import helios.util.nio.FileOps._
import helios.util.nio.AsyncFileChannel.AsyncFileChannelOps
import helios.util.gpio.GPIOPin.GPIOPin


import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

object GPIOPin extends Enumeration {
  type GPIOPin = Value
  val P8_14, P8_15, P8_16, P8_17 = Value
}


/**
 * represents a physical GPIO
 * @param pin the pin name alias
 * @param dir the directory of the pin
 */
case class GPIO(pin: GPIOPin, dir: String)

object GPIO {

  import GPIOPin._

  /**
   * Convert a gpioPin name to its software pin
   * @param gpioPin the gpioPin to convert
   * @return a string containing the software pin name
   */
  def toSwPin(gpioPin: GPIOPin) = {
    gpioPin match {
      case P8_14 => "26"
      case P8_15 => "47"
      case P8_16 => "46"
      case P8_17 => "27"
      case _ => ""
    }
  }

  private val gpioDir = "/sys/class/gpio/"
  private val export = gpioDir + "export"
  private val unexport = gpioDir + "unexport"
  private val gpioPrefix = "gpio"


  /**
   * return the full directory of a gpioPin
   * @param gpioPin the pin
   * @return the full directory in a string
   */
  private def getFullDir(gpioPin: GPIOPin): String = gpioDir + gpioPrefix + toSwPin(gpioPin)


  /**
   * return the full directory of a gpioPin
   * @param gpio the pin
   * @return the full directory in a string
   */
  private def getFullDir(gpio: GPIO): String = getFullDir(gpio.pin)

  /**
   * return the full directory of the direction directory
   * @param gpioPin the pin
   * @return the full directory in a string
   */
  private def getDirectionDir(gpioPin: GPIOPin): String = getFullDir(gpioPin) + "/direction"

  /**
   * return the full directory of the direction directory
   * @param gpio the pin
   * @return the full directory in a string
   */
  private def getDirectionDir(gpio: GPIO): String = getDirectionDir(gpio.pin)

  /**
   * return the full directory of the value directory
   * @param gpioPin the pin
   * @return the full directory in a string
   */
  private def getValueDir(gpioPin: GPIOPin): String = getFullDir(gpioPin) + "/value"

  /**
   * return the full directory of the value directory
   * @param gpio the pin
   * @return the full directory in a string
   */
  private def getValueDir(gpio: GPIO): String = getValueDir(gpio.pin)

  /**
   * export a gpioPin
   * @param gpioPin the pin
   * @param file the file to export
   * @return the created GPIO
   */
  private def ex(gpioPin: GPIOPin, file: String): GPIO = {
    writeLines(file, toSwPin(gpioPin))

    GPIO(gpioPin, getFullDir(gpioPin))
  }

  /**
   * Export a gpioPin
   * @param gpioPin the pin
   * @return Success containing the GPIO if the operation succeeded, Failure otherwise
   */
  def export(gpioPin: GPIOPin): Try[GPIO] = Try(ex(gpioPin, export))

  /**
   * Export a gpioPin
   * @param gpio the pin
   * @return Success containing the GPIO if the operation succeeded, Failure otherwise
   */
  def export(gpio: GPIO): Try[GPIO] = export(gpio.pin)

  /**
   * Unexports a pin
   * @param gpioPin the pin
   * @return Success containing the GPIO if the operation succeeded, Failure otherwise
   */
  def unexport(gpioPin: GPIOPin): Try[GPIO] = Try(ex(gpioPin, unexport))

  /**
   * Unexports a pin
   * @param gpio the pin
   * @return Success containing the GPIO if the operation succeeded, Failure otherwise
   */
  def unexport(gpio: GPIO): Try[GPIO] = unexport(gpio.pin)

  /**
   * Checks if a GPIO is exported
   * @param gpio the pin
   * @return True if the GPIO is exported
   */
  def isExported(gpio: GPIO): Boolean = exists(gpio.dir)

  /**
   * Sets the direction (in or out) of a GPIO
   * @param gpio the GPIO
   * @param input True if input, false otherwise
   * @return True if operation succeeded, False otherwise
   */
  def setDirection(gpio: GPIO, input: Boolean): Boolean = {
    val dir = input match {
      case true => "in"
      case false => "out"
    }

    writeLines(getDirectionDir(gpio), dir).isSuccess
  }

  /**
   * Returns the direction of a GPIO
   * @param gpio the GPIO
   * @return Some containing the direction if the GPIO could be read, None otherwise
   */
  def getDirection(gpio: GPIO): Option[String] = {
    readLines(getDirectionDir(gpio)).
      map(_.find(_.nonEmpty)).
      toOption.flatten
  }

  /**
   * Sets the value of a GPIO
   * @param gpio the GPIO
   * @param value either 0 (low) or 1 (high)
   * @return True if value is set succesfully
   */
  def setValue(gpio: GPIO, value: Int): Boolean = {
    writeLines(getValueDir(gpio), value.toString).isSuccess
  }

  /**
   * Gets the value of a GPIO
   * @param gpio the GPIO to check
   * @return Success containing the value in a string, if the operation succeeds
   */
  def getValue(gpio: GPIO): Try[String] = {
    readLines(getValueDir(gpio)) map (_ mkString " ")
  }

  //  private def SetEdge(gpio: GPIO, edge: String): Int = ???
  //
  //  private def Open(gpio: GPIO): Boolean = ???
  //
  //  private def Close(gpio: GPIO): Boolean = ???
}

object HeliosGPIO {

  import GPIO._

  val resetPin = export(GPIOPin.P8_17)
  val bootloaderPin = export(GPIOPin.P8_16)

  /**
   * Initialize the GPIOs used by Helios
   * @return Success containing True if initialization was successful, Success containing False if not. Failure if an exception is thrown
   */
  def initialize: Try[Boolean] = {
    bootloaderPin.map {
      g =>
        setDirection(g, input = false)
    }

    resetPin.map {
      g =>
        setDirection(g, input = false)
        setValue(g, 1)
    }
  }

  /**
   * Resets the Helios Flight Controller
   * @return A Future containing Success containing True, if reset was successful
   */
  def reset: Future[Try[Boolean]] = Future {
    resetPin.map {
      g =>
        setValue(g, 0)
        Thread.sleep(10)
        setValue(g, 1)
    }

  }
}
