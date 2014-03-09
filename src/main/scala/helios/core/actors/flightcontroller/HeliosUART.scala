package helios.core.actors.flightcontroller

import akka.actor._
import akka.util.ByteString

import com.github.jodersky.flow.Serial._
import com.github.jodersky.flow.Serial


import org.slf4j.LoggerFactory
import helios.core.actors.flightcontroller.FlightControllerMessages._

import helios.core.actors.flightcontroller.FlightControllerMessages.WriteData
import com.github.jodersky.flow.Serial.Write
import com.github.jodersky.flow.Serial.Received
import akka.actor.Terminated
import com.github.jodersky.flow.Serial.Register
import helios.core.actors.flightcontroller.FlightControllerMessages.WriteAck
import com.github.jodersky.flow.SerialSettings
import helios.core.actors.flightcontroller.FlightControllerMessages.WriteMAVLink
import com.github.jodersky.flow.Serial.CommandFailed
import com.github.jodersky.flow.Serial.Opened

object HeliosUART {
  def apply(subscriptionHandler: ActorRef, serialManager: ActorRef, settings: SerialSettings): Props =
    Props(new HeliosUART(subscriptionHandler, serialManager, settings))
}

class HeliosUART(subscriptionHandler: ActorRef, uartManager: ActorRef, settings: SerialSettings) extends Actor {

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
      logger.warn(s"Failed to register: $reason")
      throw reason //LET IT CRASH!

    case Opened(set, operator) =>
      context become opened(operator)
      context watch operator
      operator ! Register(self)
  }

  def opened(operator: ActorRef): Receive = {
    case Received(data) =>
      //TODO
      logger.debug(s"Received data: ${formatData(data)}")
      //val ml = data.toMAVLink
      //subscriptionHandler ! RawMAVLink(ml)

    case WriteData(data) =>
      val dataBs = ByteString(data.getBytes)
      operator ! Write(dataBs, WriteAck(dataBs))

    case WriteAck(data) =>
      //TODO
      logger.debug(s"WriteAck(${formatData(data)}))")

    case WriteMAVLink(msg) =>
      operator ! Write(ByteString(msg.encode()))

    case Terminated(`operator`) =>
      logger.warn("Serialport operator closed unexpectedly")
      context stop self

    case Closed =>
      logger.debug("Closed serialport")
      context stop self
  }
}



