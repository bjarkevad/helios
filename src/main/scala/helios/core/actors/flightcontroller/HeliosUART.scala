package helios.core.actors.flightcontroller

import akka.actor.{Props, ActorRef, Actor}
import akka.actor.Terminated
import akka.io.IO
import akka.util.ByteString

import com.github.jodersky.flow.Serial._
import com.github.jodersky.flow.Serial.Received
import com.github.jodersky.flow.Serial.Register
import com.github.jodersky.flow.{Serial, SerialSettings}
import com.github.jodersky.flow.Serial.CommandFailed
import com.github.jodersky.flow.Serial.Opened

import org.slf4j.LoggerFactory
import helios.core.actors.flightcontroller.FlightControllerMessages._

object HeliosUART {
  def apply(serialManager: ActorRef, settings: SerialSettings): Props =
    Props(new HeliosUART(serialManager, settings))
}

class HeliosUART(uartManager: ActorRef, settings: SerialSettings) extends Actor {

  lazy val logger = LoggerFactory.getLogger(classOf[HeliosUART])

  override def preStart() = {
    logger.debug("Prestart")
    uartManager ! Serial.Open(settings)
  }

  override def postStop() = {
    logger.debug("postStop")
  }

  override def receive: Receive = {
    case CommandFailed(cmd, reason) =>
    case Opened(set, operator) => {
      context become opened(operator)
      context watch operator
      operator ! Register(self)
    }
  }

  def opened(operator: ActorRef): Receive = {
    case Received(data) =>
      logger.debug(s"Received data: ${formatData(data)}")

    case WriteData(data) =>
      val dataBs = ByteString(data.getBytes)
      operator ! Write(dataBs, WriteAck(dataBs))

    case WriteAck(data) =>
      logger.debug(s"WroteData(${formatData(data)}))")

    case WriteMAVLink(msg) =>

    case Terminated(`operator`) =>
      logger.debug("Operator terminated")
      context stop self

    case Closed =>
      logger.debug("Closed serialport")
      context stop self
  }
}



