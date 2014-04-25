package helios.core.actors.uart

import akka.actor._
import akka.util.ByteString
import scala.util.{Try, Success}

import helios.core.actors.uart.DataMessages._
import helios.api.messages.MAVLinkMessages.PublishMAVLink

import com.github.jodersky.flow.{NoSuchPortException, Serial, SerialSettings}
import org.slf4j.LoggerFactory
import org.mavlink.messages._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import org.mavlink.MAVLinkReader

import helios.util.Subscribers._
import helios.core.actors.CoreMessages.{SetPrimary, NotAllowed, SetSubscribers, RegisterClient}
import helios.util.Privileged.PrivilegedLike

/**
 * Factory object for props
 */
object MAVLinkUART {
  def props(uartManager: ActorRef, settings: SerialSettings): Props = {
    implicit val priv = helios.util.Privileged.PrivilegedLike.PrivilegedMAVLink
    Props(new MAVLinkUART(uartManager, settings))
  }

  def props(uartManager: ActorRef, settings: SerialSettings, name: String): Props = {
    implicit val priv = helios.util.Privileged.PrivilegedLike.PrivilegedMAVLink
    Props(new MAVLinkUART(uartManager, settings))
  }
}

/**
 * Actor used to communicate over a /dev/tty* device through MAVLink
 *
 * @param uartManager
 * @param settings
 * @param mlPriv
 */
class MAVLinkUART(uartManager: ActorRef, settings: SerialSettings)
                 (implicit mlPriv: PrivilegedLike[MAVLinkMessage])
  extends Actor
  with Stash {

  lazy val logger = LoggerFactory.getLogger(classOf[MAVLinkUART])

  lazy val mlReader = new MAVLinkReader(0xFE.toByte)

  override def preStart() = {
    context.parent ! RegisterClient(self)
    uartManager ! Serial.Open(settings)
  }

  override def receive: Receive = default

  def default: Receive = {
    case Serial.CommandFailed(cmd, reason) =>
      logger.warn(s"Failed to open serial port: $reason")
      reason match {
        case _: NoSuchPortException =>
          throw reason

        case _ =>
          context.system.scheduler.scheduleOnce(100 millis, uartManager, cmd)
      }

    case Serial.Opened(set, operator) =>
      logger.debug(s"Serial port opened with settings: $set")
      context become opened(operator, context.parent)
      context watch operator
      operator ! Serial.Register(self)
      unstashAll()

    case _ =>
      stash()
  }

  def opened(operator: ActorRef, primary: ActorRef, subscribers: Subscribers = NoSubscribers): Receive = {
    //TODO: Fix this mess
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

    case WriteMAVLink(msg) if mlPriv.isPrivileged(msg) && sender != primary =>
      logger.debug(s"Sender: $sender, was not primary: $primary")
      sender ! NotAllowed(msg)

    case WriteMAVLink(msg) =>
      operator ! Serial.Write(ByteString(msg.encode()))

    case PublishMAVLink(msg) =>
      operator ! Serial.Write(ByteString(msg.encode()))

    case SetPrimary(newPrimary) =>
      logger.debug(s"Set new primary: $newPrimary")
      logger.debug(s"Sender was: $sender")
      context become opened(operator, newPrimary, subscribers)

    case Terminated(`operator`) =>
      context stop self
      throw new java.io.IOException("Serialport closed unexpectedly")

    case Serial.Closed =>
      logger.debug("Serialport closed")
      context stop self

    case SetSubscribers(subs) =>
      context become opened(operator, primary, subs)
  }
}



