package helios.core.flightcontroller

import scala.util.Try

trait _mavLinkMsg {
  def header = "HEADER"
}

case class MAVLinkMessage() extends _mavLinkMsg

trait FCComm {
  def open[T](device: T): Boolean

  def close: Boolean

  def read: Try[MAVLinkMessage]

  def write(msg: MAVLinkMessage): Boolean
}

object SPI {

  import scalaz.stream._
  import scalaz.concurrent.Task

  def read[T](path: String)(lineRead: String => T): Task[Unit] = {
    io.linesR(path).map(lineRead(_)).run
  }

  //  def close: Boolean = ???
  //  def read: Try[MAVLinkMessage] = ???
  //  def write(msg: MAVLinkMessage): Boolean = ???

}

object TCP extends FCComm {
  override def close: Boolean = ???

  override def open[T](device: T): Boolean = ???

  override def read: Try[MAVLinkMessage] = ???

  override def write(msg: MAVLinkMessage): Boolean = ???

}
