package helios.core.actors

import akka.actor._
import akka.actor.SupervisorStrategy._

import language.postfixOps
import concurrent.duration._

import helios.core.actors.uart.DataMessages.WriteMAVLink
import helios.api.HeliosAPI
import helios.api.messages.MAVLinkMessages.PublishMAVLink
import helios.api.HeliosRemote.RegisterAPIClient

import org.slf4j.LoggerFactory
import com.github.jodersky.flow.{NoSuchPortException, PortInUseException}
import helios.core.HeliosAPIDefault
import org.mavlink.messages.MAVLinkMessage
import helios.util.Subscribers.Subscribers
import scala.collection.GenSetLike

object ClientReceptionist {
  val defaultStrategy =
        OneForOneStrategy(maxNrOfRetries = 100,
      withinTimeRange = 1 seconds,
      loggingEnabled = true) {
      case _: java.io.IOException => Restart
      case _: PortInUseException => Restart
      case _: NoSuchPortException => Stop
      case _: NotImplementedError => Resume
      case e => Restart
    }

  def props(clientProps: Iterable[(Props, String)], supervisorStrategy: SupervisorStrategy = defaultStrategy): Props =
    Props(new ClientReceptionist(clientProps, supervisorStrategy))
}

class ClientReceptionist(clientProps: Iterable[(Props, String)], supervisorStrategy: SupervisorStrategy) extends Actor {

  import CoreMessages._

  val logger = LoggerFactory.getLogger(classOf[ClientReceptionist])

  //TODO: fix supervision strategy for UARTS
  override val supervisorStrategy = supervisorStrategy

  val clients = clientProps.map(p => context watch context.actorOf(p._1, p._2))

  def updateSubscriptions(activeClients: Iterable[ActorRef]) {
    activeClients foreach(c => c ! SetSubscribers(activeClients.filter(!_.equals(c))))
  }

  override def preStart() = {
    logger.debug(context.self.path.toString)
  }

  def receive = defaultReceive()

  //TODO: uart ! SetPrimary ???
  def defaultReceive(activeClients: Set[ActorRef] = Set.empty): Receive = {
    case RegisterClient() =>
      val cs = sender
      val ac = activeClients + cs
      context become defaultReceive(ac)
      cs ! Registered()
      updateSubscriptions(ac)

    case UnregisterClient() if activeClients contains sender =>
      val cs = sender
      context become defaultReceive(activeClients - cs)
      cs ! Unregistered()

    case RegisterAPIClient(c) =>
      val hd: HeliosAPI =
        TypedActor(context.system)
          .typedActorOf(TypedProps(
          classOf[HeliosAPI], new HeliosAPIDefault("HeliosDefault", self, c, uart, muxUart, 20)))

      val cs = TypedActor(context.system).getActorRefFor(hd)
      val ac = activeClients + cs
      context become defaultReceive(ac)

      sender ! hd
      updateSubscriptions(ac)

    case m@PublishMAVLink(ml) =>
      logger.error("ClientReceptionist should not receive PublishMAVLink messages!!")

    case WriteMAVLink(m) =>
      logger.error("ClientReceptionist should not receive WriteMAVLink messages!!")
  }
}

object CoreMessages {

  trait Request

  trait Response

  case class RegisterClient() extends Request

  case class UnregisterClient() extends Request

  case class Registered() extends Response

  case class RegisteredAPI(helios: HeliosAPI) extends Response

  case class Unregistered() extends Response

  case class SetPrimary(newPrimary: ActorRef) extends Response

  case class NotAllowed(msg: MAVLinkMessage) extends Response

  case class SetSubscribers(subscribers: Subscribers) extends Response
}
