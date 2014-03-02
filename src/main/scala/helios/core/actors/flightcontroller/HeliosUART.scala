package helios.core.actors.flightcontroller

import akka.actor._
import akka.actor.Terminated
import akka.io.IO
import akka.util.ByteString

import com.github.jodersky.flow.Serial._
import com.github.jodersky.flow.Serial.Received
import com.github.jodersky.flow.Serial.Register
import com.github.jodersky.flow.{Serial, SerialSettings}
import com.github.jodersky.flow.Serial.CommandFailed
import com.github.jodersky.flow.Serial.Opened

import helios.core.actors.flightcontroller.FlightControllerMessages._

class HeliosUART(settings: SerialSettings) extends Actor {
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
    case Opened(settings, operator) => {
      //val operator = sender
      context become opened(operator)
      context watch operator
      operator ! Register(self)
    }
  }

  def opened(operator: ActorRef): Receive = {
    case Received(data) =>
      println(s"Received data: ${formatData(data)}")

    case WriteData(data) =>
      val dataBs = ByteString(data.getBytes)
      //println(s"Writing: ${formatData(dataBs)}")
      operator ! Write(dataBs, WroteData(dataBs))

    case WroteData(data) =>
      //println(s"Wrote data: ${formatData(data)}")

    case WriteMAVLink(msg) =>

    case Terminated(`operator`) =>
      println("Operator terminated")
      context stop self

    case Closed =>
      println("Closed serialport")
      context stop self
  }
}

object HeliosUART {
  def apply(settings: SerialSettings): Props = Props(classOf[HeliosUART], settings)
}

