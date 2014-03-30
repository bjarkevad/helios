package helios.core.actors.flightcontroller

import akka.actor._
import akka.util.ByteString
import scala.util.{Failure, Success}

import helios.core.actors.flightcontroller.FlightControllerMessages._
import helios.mavlink.MAVLink.convertToMAVLink
import helios.api.messages.MAVLinkMessages.PublishMAVLink

import com.github.jodersky.flow.{Serial, SerialSettings}
import org.slf4j.LoggerFactory
import org.mavlink.messages.IMAVLinkMessageID._
import org.mavlink.messages.{MAV_CMD, MAVLinkMessage}
import org.mavlink.messages.common.msg_command_long
import scala.collection.mutable
object HeliosUART {
  def props(uartManager: ActorRef, settings: SerialSettings): Props = {
    Props(new HeliosUART(uartManager, settings))
  }

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

  implicit class subscriberImpls(val subs: Seq[ActorRef]) {
    def !(msg: Any)(implicit sender: ActorRef) = subs.foreach(_ ! msg)
  }

  case class SetPrimary(newPrimary: ActorRef)

  case class NotAllowed(msg: MAVLinkMessage)

}

class HeliosUART(uartManager: ActorRef, settings: SerialSettings) extends Actor {

  import HeliosUART._

  lazy val logger = LoggerFactory.getLogger(classOf[HeliosUART])

  var messageBuffer: ByteString = ByteString()
  var nextLen: Int = 0

  override def preStart() = {
    logger.debug(s"Started with serial settings: $settings and parent ${context.parent}")
    uartManager ! Serial.Open(settings)
  }

  override def postStop() = {
  }

  override def receive: Receive = {
    case Serial.CommandFailed(cmd, reason) =>
      logger.warn(s"Failed to open serial port: $reason")
      throw reason //LET IT CRASH!

    case Serial.Opened(set, operator) =>
      logger.debug(s"Serial port opened with settings: $set and parent: ${context.parent}")
      context become opened(operator, context.parent, Seq(context.parent))
      context watch operator
      operator ! Serial.Register(self)
  }

  //import HeliosUART.subscriberImpls
  def opened(operator: ActorRef, primary: ActorRef, subscribers: Seq[ActorRef]): Receive = {
    case Serial.Received(data) =>
      logger.debug(s"Received: $data")
      var ok = false
      if(data(0) == -2 && messageBuffer.isEmpty) {
        nextLen = data(1) + 8
        println("message started")
        messageBuffer = data
        ok = true
      }
      else if (messageBuffer.length < nextLen) {
        println("more of a message")
        messageBuffer = messageBuffer ++ data
        ok = true
      }

      if(messageBuffer.length >= nextLen) {
        println(s"message done $messageBuffer")

        convertToMAVLink(messageBuffer) match {
          case Success(m) => subscribers ! PublishMAVLink(m)
          case Failure(e: Throwable) => logger.warn(s"Received something unknown over UART: $e")
          case e@_ => logger.warn(s"What the heck? $e")
        }

        messageBuffer = ByteString()
        ok = true
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
      logger.debug(s"Writing MAVLink to UART: $msg")
      operator ! Serial.Write(ByteString(msg.encode()))

    case SetPrimary(newPrimary) =>
      logger.debug(s"Set new primary: $newPrimary")
      logger.debug(s"Sender was: $sender")
      context become opened(operator, newPrimary, subscribers)

    case Terminated(`operator`) =>
      logger.warn("Serialport operator closed unexpectedly")
      context stop self

    case Serial.Closed =>
      logger.debug("Closed serialport")
      context stop self
  }
}



