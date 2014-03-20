package helios.core.actors.flightcontroller

import akka.actor._
import akka.util.ByteString

import org.slf4j.LoggerFactory
import helios.core.actors.flightcontroller.FlightControllerMessages._
import helios.mavlink.MAVLink.convertToMAVLink

import com.github.jodersky.flow.Serial
import com.github.jodersky.flow.SerialSettings
import scala.util.{Failure, Success}
import helios.api.messages.MAVLinkMessages.PublishMAVLink

import org.mavlink.messages.IMAVLinkMessageID._
import org.mavlink.messages.{MAV_CMD, MAVLinkMessage}
import org.mavlink.messages.common.msg_command_long

object HeliosUART {
  def props(subscriptionHandler: ActorRef, serialManager: ActorRef, settings: SerialSettings): Props =
    Props(new HeliosUART(subscriptionHandler, serialManager, settings))

  lazy val privilegedMessages: Set[Int] = Set(
    MAVLINK_MSG_ID_SET_ROLL_PITCH_YAW_SPEED_THRUST,
    MAVLINK_MSG_ID_SET_ROLL_PITCH_YAW_THRUST
  )

  lazy val privilegedCommands: Set[Int] = Set(
    MAV_CMD.MAV_CMD_NAV_LAND,
    MAV_CMD.MAV_CMD_NAV_TAKEOFF
  )

  implicit class MAVLinkMessageImpl(val mlmsg: MAVLinkMessage) {
    def isPrivileged: Boolean = {
      privilegedMessages.contains(mlmsg.messageType) ||
        (mlmsg match {
          case m: msg_command_long =>
            privilegedCommands.contains(m.command)
          case _ =>
            false
        })
    }
  }

  case class SetPrimary(newPrimary: ActorRef)

  case class NotAllowed(msg: MAVLinkMessage)

}

class HeliosUART(subscriptionHandler: ActorRef, uartManager: ActorRef, settings: SerialSettings) extends Actor {

  import HeliosUART._

  lazy val logger = LoggerFactory.getLogger(classOf[HeliosUART])

  override def preStart() = {
    uartManager ! Serial.Open(settings)
  }

  override def postStop() = {
  }

  override def receive: Receive = {
    case Serial.CommandFailed(cmd, reason) =>
      logger.warn(s"Failed to open serial port: $reason")
      throw reason //LET IT CRASH!

    case Serial.Opened(set, operator) =>
      context become opened(operator, context.parent)
      context watch operator
      operator ! Serial.Register(self)
  }

  def opened(operator: ActorRef, primary: ActorRef): Receive = {
    case Serial.Received(data) =>
      convertToMAVLink(data) match {
        case Success(m) => subscriptionHandler ! PublishMAVLink(m)
        case Failure(e: Throwable) => logger.warn(s"Received something unknown over UART: $e")
        case e@_ => logger.warn(s"What the heck? $e")
      }

    case WriteData(data) =>
      val dataBs = ByteString(data.getBytes)
      operator ! Serial.Write(dataBs, WriteAck(dataBs))

    case WriteAck(data) =>
      //TODO
      logger.debug(s"WriteAck(${formatData(data)}))")

    case WriteMAVLink(msg) if msg.isPrivileged && sender != primary =>
      logger.debug(s"Sender: $sender, was not primary: $primary")
      sender ! NotAllowed(msg)

    case WriteMAVLink(msg) =>
      operator ! Serial.Write(ByteString(msg.encode()))

    case SetPrimary(newPrimary) =>
      logger.debug(s"Set new primary: $newPrimary")
      logger.debug(s"Sender was: $sender")
      context become opened(operator, newPrimary)

    case Terminated(`operator`) =>
      logger.warn("Serialport operator closed unexpectedly")
      context stop self

    case Serial.Closed =>
      logger.debug("Closed serialport")
      context stop self
  }
}



