package helios.core.actors.flightcontroller

import akka.actor._
import akka.util.ByteString
import scala.util.{Try, Failure, Success}

import helios.core.actors.flightcontroller.FlightControllerMessages._
import helios.api.messages.MAVLinkMessages.PublishMAVLink

import com.github.jodersky.flow.{NoSuchPortException, Serial, SerialSettings}
import org.slf4j.LoggerFactory
import org.mavlink.messages.IMAVLinkMessageID._
import org.mavlink.messages.{MAV_CMD, MAVLinkMessage}
import org.mavlink.messages.common.msg_command_long

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import org.mavlink.MAVLinkReader

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

  implicit class subscriberImpls(val subs: Set[ActorRef]) {
    def !(msg: Any)(implicit sender: ActorRef) = {
      subs.foreach(_ ! msg)
    }
  }

  case class SetPrimary(newPrimary: ActorRef)

  case class NotAllowed(msg: MAVLinkMessage)

  trait subscriptionEvent

  case class AddSubscriber(actor: ActorRef) extends subscriptionEvent

  case class RemoveSubscriber(actor: ActorRef) extends subscriptionEvent

  lazy val testCmd = {
    val msg = new msg_command_long(20, 0)
    msg.command = MAV_CMD.MAV_CMD_NAV_TAKEOFF
    msg.param1 = 0 //pitch
    msg.param4 = 0 //yaw
    msg.param5 = 0 //latitude
    msg.param6 = 0 //longtitude
    msg.param7 = 2 //altitude

    msg
  }

}

class HeliosUART(uartManager: ActorRef, settings: SerialSettings) extends Actor
with Stash {

  import HeliosUART._

  lazy val logger = LoggerFactory.getLogger(classOf[HeliosUART])

  lazy val mlReader = new MAVLinkReader(0xFE.toByte)

  override def preStart() = {
    logger.debug(s"Started with serial settings: $settings and parent ${context.parent}")
    uartManager ! Serial.Open(settings)
  }

  override def postStop() = {
  }

  override def receive: Receive = default


  def default: Receive = {
    case Serial.CommandFailed(cmd, reason) =>
      logger.warn(s"Failed to open serial port: $reason")
      reason match {
        case _: NoSuchPortException => throw reason
        case _ =>
          context.system.scheduler.scheduleOnce(100 millis, uartManager, cmd) //retry after 100 ms
      }

    case Serial.Opened(set, operator) =>
      logger.debug(s"Serial port opened with settings: $set and parent: ${context.parent}")
      context become opened(operator, context.parent, Set.empty)
      context watch operator
      operator ! Serial.Register(self)
      unstashAll()
      context.system.scheduler.schedule(1 second, 1 second, operator, Serial.Write(ByteString(testCmd.encode())))

    case _: subscriptionEvent =>
      stash()
  }

  //import HeliosUART.subscriberImpls
  def opened(operator: ActorRef, primary: ActorRef, subscribers: Set[ActorRef]): Receive = {
    case Serial.Received(data) =>
      //     logger.debug(s"Received: $data")
      Try(mlReader.getNextMessage(data.toArray, data.length)) match {
        case Success(msg: MAVLinkMessage) =>
          //logger.debug("Got MAVLink message!")
          subscribers ! PublishMAVLink(msg)

          var moreMessages: Boolean = true
          while (moreMessages) {
            Try(mlReader.getNextMessage) match {
              case Success(msg: MAVLinkMessage) =>
                //logger.debug("Sent extra message")
                subscribers ! PublishMAVLink(msg)
              case _ =>
                moreMessages = false
            }
          }


        case _ =>
          //logger.debug("Message not yet finished..")
      }

      Thread.sleep(2)

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
      logger.debug(s"Writing MAVLink to UART: $msg from $sender")
      operator ! Serial.Write(ByteString(msg.encode()))

    case SetPrimary(newPrimary) =>
      logger.debug(s"Set new primary: $newPrimary")
      logger.debug(s"Sender was: $sender")
      context become opened(operator, newPrimary, subscribers)

    case Terminated(`operator`) =>
      //logger.warn("Serialport operator closed unexpectedly")
      throw new java.io.IOException("Serialport closed unexpectedly")

    case Serial.Closed =>
      logger.debug("Closed serialport")
      context stop self

    case AddSubscriber(actor) =>
      context become opened(operator, primary, subscribers + actor)

    case RemoveSubscriber(actor) =>
      context become opened(operator, primary, subscribers - actor)
  }
}



