package helios.core.actors

import akka.actor._
import akka.actor.ActorSystem
import akka.actor.Terminated
import akka.io.IO
import akka.util.ByteString

import com.github.jodersky.flow.Serial._
import com.github.jodersky.flow.Serial.Received
import com.github.jodersky.flow.Serial.Register
import com.github.jodersky.flow.{Serial, SerialSettings}
import com.github.jodersky.flow.Serial.CommandFailed
import com.github.jodersky.flow.Serial.Opened

import helios.core.actors.SerialPort._


class SerialPort(settings: SerialSettings) extends Actor {
  import context._

  override def preStart() = {
    println("Prestart")
    IO(Serial) ! Serial.Open(settings)
  }

  override def postStop() = {
    println("Poststop")
  }

  override def receive: Receive = {
    case CommandFailed(cmd, reason) =>
    case Opened(s, o) => {
      val operator = sender
      context become opened(operator)
      context watch operator
      operator ! Register(self)
    }
  }

  def opened(operator: ActorRef): Receive = {
    case Received(data) =>
      println(s"Received data: ${formatData(data)}")

    case Terminated(`operator`) =>
      println("Operator terminated")
      context stop self

    case WriteStuff(data) =>
      println(s"Writing: ${formatData(data)}")
      operator ! Serial.Write(data,Wrote(data))

    case Wrote(data) =>
      println(s"Wrote data: ${formatData(data)}")

    case Closed =>
      println("Closed serialport")
      context stop self
  }

}

object SerialPort {
  case class Wrote(data: ByteString) extends Event
  case class WriteStuff(data: ByteString) extends Event
  //case class Write(data: ByteString) extends Event

  def apply(settings: SerialSettings) = Props(classOf[SerialPort], settings)

  private def formatData(data: ByteString) = data.mkString("[", ",", "]") + " " + (new String(data.toArray, "UTF-8"))
}
