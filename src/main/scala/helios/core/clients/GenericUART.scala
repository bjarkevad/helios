package helios.core.clients

import akka.actor.{Terminated, ActorRef, Props}
import akka.util.ByteString
import com.github.jodersky.flow.{NoSuchPortException, Serial, SerialSettings}
import org.slf4j.LoggerFactory
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import helios.messages.CoreMessages.{UnregisterClient, RegisterClient, SetSubscribers}
import Clients.{ClientTypeProvider, Client}
import helios.types.Subscribers
import helios.core.clients.DataMessages.WriteData

object GenericUART {
  def props(clientTypeProvider: ClientTypeProvider, uartManager: ActorRef, settings: SerialSettings): Props =
    Props(new GenericUART(clientTypeProvider, uartManager, settings))
}

//TODO: Register
class GenericUART(override val clientTypeProvider: ClientTypeProvider, uartManager: ActorRef, settings: SerialSettings)
  extends Client {

  import Subscribers._

  lazy val logger = LoggerFactory.getLogger(classOf[GenericUART])

  override def preStart() = {
    uartManager ! Serial.Open(settings)
  }

  override def postStop() = {
    context.parent ! UnregisterClient(clientType)
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
      context become opened(operator)
      context watch operator
      context.parent ! RegisterClient(clientType)
      operator ! Serial.Register(self)
  }

  def opened(operator: ActorRef, subscribers: Subscribers = NoSubscribers): Receive = {
    case m@Serial.Received(data) =>
      subscribers ! m

    case WriteData(data) =>
      operator ! Serial.Write(ByteString(data))

    case Terminated(`operator`) =>
      throw new java.io.IOException("Serialport closed unexpectedly")

    case Serial.Closed =>
      logger.debug("Serialport closed")
      context stop self

    case SetSubscribers(subs) =>
      context become opened(operator, subs)
  }
}
