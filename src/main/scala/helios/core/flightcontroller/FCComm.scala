package helios.core.flightcontroller

import scala.util.Try

trait _mavLinkMsg {
  def header = "HEADER"
}

case class MAVLinkMessage() extends _mavLinkMsg

trait FCComm {
  def open: Boolean
  def close: Boolean
  def read: Try[MAVLinkMessage]
  def write(msg: MAVLinkMessage): Boolean
}

object SPI extends FCComm {
  override def open: Boolean = ???
  override def close: Boolean = ???
  override def read: Try[MAVLinkMessage] = ???
  override def write(msg: MAVLinkMessage): Boolean = ???

}

object TCP extends FCComm {
  override def close: Boolean = ???
  override def open: Boolean = ???
  override def read: Try[MAVLinkMessage] = ???
  override def write(msg: MAVLinkMessage): Boolean = ???

}
