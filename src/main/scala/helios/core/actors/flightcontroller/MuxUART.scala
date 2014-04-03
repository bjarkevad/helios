package helios.core.actors.flightcontroller

import akka.actor.{Terminated, ActorRef, Props, Actor}
import com.github.jodersky.flow.{NoSuchPortException, Serial, SerialSettings}
import org.slf4j.LoggerFactory
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.util.ByteString
import scala.concurrent.ExecutionContext.Implicits.global

object MuxUART {
  case class WriteData(data: ByteString)
  def props(uartManager: ActorRef, settings: SerialSettings): Props = Props(new MuxUART(uartManager, settings))
}

class MuxUART(uartManager: ActorRef, settings: SerialSettings) extends Actor {

  import MuxUART._

  lazy val logger = LoggerFactory.getLogger(classOf[MuxUART])

  override def preStart() = {
    logger.debug("Started MuxUART")
    uartManager ! Serial.Open(settings)
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
      context become opened(operator, Set.empty)
      context watch operator
      operator ! Serial.Register(self)
  }

  import helios.util.Subscribers._
  def opened(operator: ActorRef, subscribers: Set[ActorRef]): Receive = {
    case m@Serial.Received(data) =>
      subscribers ! m

    case WriteData(data) =>
      operator ! Serial.Write(data)

    case Terminated(`operator`) =>
      throw new java.io.IOException("Serialport closed unexpectedly")

    case Serial.Closed =>
      logger.debug("Closed serialport")
      context stop self

    case MAVLinkUART.SetSubscribers(subs) =>
      context become opened(operator, subs)
  }
}
