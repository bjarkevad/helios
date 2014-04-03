package helios.core.actors.groundcontrol

import akka.actor._
import com.github.jodersky.flow.{NoSuchPortException, Serial, SerialSettings}
import org.slf4j.LoggerFactory
import org.mavlink.MAVLinkReader
import helios.core.actors.flightcontroller.MAVLinkUART.{RemoveSubscriber, AddSubscriber, SubscriptionEvent}
import helios.util.Subscribers._
import scala.concurrent.duration._
import helios.core.actors.flightcontroller.FlightControllerMessages.WriteMAVLink
import akka.util.ByteString

object GroundControlUART {
  def props(uartManager: ActorRef, settings: SerialSettings): Props = {
    Props(new GroundControlUART(uartManager, settings))
  }
}

//TODO: Remove
class GroundControlUART(uartManager: ActorRef, settings: SerialSettings) extends Actor
with Stash {

  ???
  import scala.language.postfixOps
  lazy val logger = LoggerFactory.getLogger(classOf[GroundControlUART])

  lazy val mlReader = new MAVLinkReader(0xFE.toByte)

  override def preStart() = {
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
      context become opened(operator, Set.empty)
      context watch operator
      operator ! Serial.Register(self)
      unstashAll()

    case _: SubscriptionEvent =>
      stash()
  }

  def opened(operator: ActorRef, subscribers: Set[ActorRef]): Receive = {
    case WriteMAVLink(msg) =>
      operator ! Serial.Write(ByteString(msg.encode()))

    case AddSubscriber(actor) =>
      context become opened(operator, subscribers + actor)

    case RemoveSubscriber(actor) =>
      context become opened(operator, subscribers - actor)
  }
}
