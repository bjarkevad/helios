package helios.core.actors.flightcontroller

import akka.actor._
import akka.util.ByteString
import scala.util.{Try, Failure, Success}

import helios.core.actors.flightcontroller.FlightControllerMessages._
import helios.api.messages.MAVLinkMessages.PublishMAVLink

import com.github.jodersky.flow.{NoSuchPortException, Serial, SerialSettings}
import org.slf4j.LoggerFactory
import org.mavlink.messages.IMAVLinkMessageID._
import org.mavlink.messages._
import org.mavlink.messages.common._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import org.mavlink.MAVLinkReader

import helios.util.Subscribers._
import helios.core.actors.CoreMessages.RegisterClient

object MAVLinkUART {
  def props(uartManager: ActorRef, settings: SerialSettings): Props = {
    Props(new MAVLinkUART(uartManager, settings))
  }

  def props(uartManager: ActorRef, settings: SerialSettings, name: String): Props = {
    Props(new MAVLinkUART(uartManager, settings))
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

  trait SubscriptionEvent

  case class SetSubscribers(subscribers: Set[ActorRef]) extends SubscriptionEvent


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
  var s = 0

  def heartbeat: MAVLinkMessage = {
    val hb = new msg_heartbeat(20, 0)
    hb.sequence = s
    s += 1
    hb.`type` = MAV_TYPE.MAV_TYPE_QUADROTOR
    hb.autopilot = MAV_AUTOPILOT.MAV_AUTOPILOT_GENERIC
    hb.base_mode = MAV_MODE.MAV_MODE_STABILIZE_ARMED
    hb.custom_mode = 0
    hb.system_status = MAV_STATE.MAV_STATE_STANDBY
    hb.mavlink_version = 3

    hb
  }

}

class MAVLinkUART(uartManager: ActorRef, settings: SerialSettings) extends Actor
with Stash {

  import MAVLinkUART._

  lazy val logger = LoggerFactory.getLogger(classOf[MAVLinkUART])

  lazy val mlReader = new MAVLinkReader(0xFE.toByte)

  override def preStart() = {
    //logger.debug(s"Started with serial settings: $settings and parent ${context.parent}")
    context.parent ! RegisterClient(self)
    uartManager ! Serial.Open(settings)
  }

  override def postStop() = {
  }

  override def receive: Receive = default


  def default: Receive = {
    case Serial.CommandFailed(cmd, reason) =>
      logger.warn(s"Failed to open serial port: $reason")
      reason match {
        case _: NoSuchPortException =>
          throw reason

        case _ =>
          context.system.scheduler.scheduleOnce(100 millis, uartManager, cmd) //retry after 100 ms
      }

    case Serial.Opened(set, operator) =>
      logger.debug(s"Serial port opened with settings: $set")
      context become opened(operator, context.parent, Set.empty)
      context watch operator
      operator ! Serial.Register(self)
      //      context.system.scheduler.schedule(0 seconds, 500 millis, self, PublishMAVLink(heartbeat))
      unstashAll()

    case _: SubscriptionEvent =>
      stash()
  }

  //import HeliosUART.subscriberImpls
  def opened(operator: ActorRef, primary: ActorRef, subscribers: Set[ActorRef]): Receive = {
    case Serial.Received(data) =>
      Try(mlReader.getNextMessage(data.toArray, data.length)) match {
        case Success(msg: MAVLinkMessage) =>
          subscribers ! PublishMAVLink(msg)

          var moreMessages: Boolean = true
          while (moreMessages) {
            Try(mlReader.getNextMessage) match {
              case Success(msg: MAVLinkMessage) =>
                subscribers ! PublishMAVLink(msg)
              case _ =>
                moreMessages = false
            }
          }


        case _ =>
      }

      //Thread.sleep(2)

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
      //logger.debug(s"Writing MAVLink to UART: $msg from $sender")
      operator ! Serial.Write(ByteString(msg.encode()))

    case PublishMAVLink(msg) =>
      //logger.debug(s"${self.path}, got: $msg")
      operator ! Serial.Write(ByteString(msg.encode()))

    case SetPrimary(newPrimary) =>
      logger.debug(s"Set new primary: $newPrimary")
      logger.debug(s"Sender was: $sender")
      context become opened(operator, newPrimary, subscribers)

    case Terminated(`operator`) =>
      context stop self
      throw new java.io.IOException("Serialport closed unexpectedly")

    case Serial.Closed =>
      logger.debug("Closed serialport")
      context stop self

    case SetSubscribers(subs) =>
      context become opened(operator, primary, subs)
  }
}



