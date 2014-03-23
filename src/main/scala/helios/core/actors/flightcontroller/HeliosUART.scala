package helios.core.actors.flightcontroller

import akka.actor._
import akka.util.ByteString
import scala.util.{Failure, Success}

import helios.core.actors.flightcontroller.FlightControllerMessages._
import helios.mavlink.MAVLink.convertToMAVLink
import helios.api.messages.MAVLinkMessages.PublishMAVLink
import helios.HeliosConfig

import com.github.jodersky.flow.{Parity, Serial, SerialSettings}
import org.slf4j.LoggerFactory
import org.mavlink.messages.IMAVLinkMessageID._
import org.mavlink.messages.{MAV_CMD, MAVLinkMessage}
import org.mavlink.messages.common.msg_command_long
import scala.collection.mutable

object HeliosUART {
  def props(subscriptionHandler: ActorRef, uartManager: ActorRef): Props = {
    Props(new HeliosUART(subscriptionHandler, uartManager))
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

  case class SetPrimary(newPrimary: ActorRef)

  case class NotAllowed(msg: MAVLinkMessage)

}

class HeliosUART(subscriptionHandler: ActorRef, uartManager: ActorRef) extends Actor {

  import HeliosUART._

  //implicit val system = context.system

  lazy val logger = LoggerFactory.getLogger(classOf[HeliosUART])

  var messageBuffer: ByteString = ByteString()
  var nextLen: Int = 0
  //  lazy val uartManager: ActorRef = {
  //    HeliosConfig.serialdevice match {
  //      case Some(_) => IO(Tcp)
  //      case None => context.actorOf(MockSerial.props)
  //    }
  //  }
  lazy val settings: SerialSettings = {
    SerialSettings(
      HeliosConfig.serialdevice.getOrElse("/dev/ttyUSB0"),
      HeliosConfig.serialBaudrate.getOrElse(115200),
      8,
      false,
      Parity(0)
    )
  }

  override def preStart() = {
    logger.debug(s"Serial settings: $settings")
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
          case Success(m) => subscriptionHandler ! PublishMAVLink(m)
          case Failure(e: Throwable) => logger.warn(s"Received something unknown over UART: $e")
          case e@_ => logger.warn(s"What the heck? $e")
        }

        messageBuffer = ByteString()
        ok = true
      }

      println(s"Ok: $ok")

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



