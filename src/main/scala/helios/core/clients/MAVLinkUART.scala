package helios.core.clients

import akka.actor._
import akka.util.ByteString
import scala.util.{Try, Success}

import helios.messages.DataMessages.PublishMAVLink

import com.github.jodersky.flow.{NoSuchPortException, Serial, SerialSettings}
import org.slf4j.LoggerFactory
import org.mavlink.messages._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import org.mavlink.MAVLinkReader

import helios.types.Subscribers
import Subscribers._
import helios.messages.CoreMessages._
import helios.util.Privileged.PrivilegedLike
import helios.core.clients.DataMessages._

object MAVLinkUART {
  def props(clientTypeFactory: ActorRef => ClientType, uartManager: ActorRef, settings: SerialSettings): Props = {
    implicit val priv = helios.util.Privileged.PrivilegedLike.PrivilegedMAVLink
    Props(new MAVLinkUART(clientTypeFactory, uartManager, settings))
  }

  def props(clientTypeFactory: ActorRef => ClientType, uartManager: ActorRef, settings: SerialSettings, name: String): Props = {
    implicit val priv = helios.util.Privileged.PrivilegedLike.PrivilegedMAVLink
    Props(new MAVLinkUART(clientTypeFactory, uartManager, settings))
  }
}

class MAVLinkUART(clientTypeFactory: ActorRef => ClientType, uartManager: ActorRef, settings: SerialSettings)
                 (implicit mlPriv: PrivilegedLike[MAVLinkMessage]) extends Actor with Stash {

  lazy val logger = LoggerFactory.getLogger(classOf[MAVLinkUART])

  lazy val mlReader = new MAVLinkReader(0xFE.toByte)

  override def preStart() = {
    context.parent ! RegisterClient(clientTypeFactory(self))
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
          context.system.scheduler.scheduleOnce(100 millis, uartManager, cmd)
      }

    case Serial.Opened(set, operator) =>
      logger.debug(s"Serial port opened with settings: $set")
      context become opened(operator, context.parent, Set.empty)
      context watch operator
      operator ! Serial.Register(self)
      unstashAll()

    case _: SetSubscribers =>
      stash()
  }

  def opened(operator: ActorRef, primary: ActorRef, subscribers: Subscribers): Receive = {
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

    //Thread.sleep(2)

    case WriteData(data) =>
      val dataBs = ByteString(data.getBytes)
      operator ! Serial.Write(dataBs, WriteAck(dataBs))

    case WriteAck(data) =>
    //TODO
    //logger.debug(s"WriteAck(${formatData(data)}))")

    case WriteMAVLink(msg) if mlPriv.isPrivileged(msg) && sender != primary =>
      logger.debug(s"Sender: $sender, was not primary: $primary")
      sender ! NotAllowed(msg)

    case WriteMAVLink(msg) =>
      logger.debug(s"Writing MAVLink to UART: $msg from $sender")
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



