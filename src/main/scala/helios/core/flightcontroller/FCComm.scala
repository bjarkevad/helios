package helios.core.flightcontroller

import com.sun.jna._

trait FCComm {
  def open: Boolean
  def close: Boolean
  def read[T]: T
  def write[T](msg: T): Boolean
}

object SPI extends FCComm {
  override def open: Boolean = ???
  override def close: Boolean = ???
  override def read[T]: T = ???
  override def write[T](msg: T): Boolean = ???

}

object TCP extends FCComm {
  override def close: Boolean = ???
  override def open: Boolean = ???
  override def read[T]: T = ???
  override def write[T](msg: T): Boolean = ???

}
