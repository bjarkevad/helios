package helios.core.actors.flightcontroller

import akka.actor._
import akka.util.ByteString

import org.slf4j.LoggerFactory
import helios.core.actors.flightcontroller.FlightControllerMessages._
import helios.mavlink.MAVLink.convertToMAVLink

import com.github.jodersky.flow.Serial._
import com.github.jodersky.flow.Serial
import com.github.jodersky.flow.SerialSettings
import scala.util.{Failure, Success}
import helios.api.messages.MAVLinkMessages.PublishMAVLink

import org.mavlink.messages.IMAVLinkMessageID._
import org.mavlink.messages.MAVLinkMessage

object HeliosUART {
  def props(subscriptionHandler: ActorRef, serialManager: ActorRef, settings: SerialSettings): Props =
    Props(new HeliosUART(subscriptionHandler, serialManager, settings))

  lazy val privilegedMessages: Set[Int] = Set(
    MAVLINK_MSG_ID_SET_ROLL_PITCH_YAW_SPEED_THRUST,
    MAVLINK_MSG_ID_SET_ROLL_PITCH_YAW_THRUST
  )

  case class NotAllowed(msg: MAVLinkMessage)

}

class HeliosUART(subscriptionHandler: ActorRef, uartManager: ActorRef, settings: SerialSettings) extends Actor {

  import HeliosUART._

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
      context become opened(operator, context.parent)
      context watch operator
      operator ! Register(self)
  }

  def opened(operator: ActorRef, primary: ActorRef): Receive = {
    case Received(data) =>
      convertToMAVLink(data) match {
        case Success(m) => subscriptionHandler ! PublishMAVLink(m)
        case Failure(e: Throwable) => logger.warn(s"Received something unknown over UART: $e")
        case e@_ => logger.warn(s"What the heck? $e")
      }

    case WriteData(data) =>
      val dataBs = ByteString(data.getBytes)
      operator ! Write(dataBs, WriteAck(dataBs))

    case WriteAck(data) =>
      //TODO
      logger.debug(s"WriteAck(${formatData(data)}))")

    case WriteMAVLink(msg) =>
      if (privilegedMessages.contains(msg.messageType) && sender != primary)
        sender ! NotAllowed(msg)
      else
        operator ! Write(ByteString(msg.encode()))

    case Terminated(`operator`) =>
      logger.warn("Serialport operator closed unexpectedly")
      context stop self

    case Closed =>
      logger.debug("Closed serialport")
      context stop self
  }
}



