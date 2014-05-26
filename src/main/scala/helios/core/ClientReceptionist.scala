package helios.core

import akka.actor._
import akka.actor.SupervisorStrategy._

import language.postfixOps
import concurrent.duration._
import helios.api.{HeliosAPIDefault, HeliosAPI}
import helios.messages.DataMessages.PublishMAVLink
import helios.messages.CoreMessages
import CoreMessages._

import org.slf4j.LoggerFactory
import com.github.jodersky.flow.{NoSuchPortException, PortInUseException}
import helios.types.Subscribers.Subscribers
import helios.core.clients.DataMessages.WriteMAVLink
import helios.types.ClientTypes._
import helios.messages.CoreMessages.RegisterClient
import helios.messages.CoreMessages.UnregisterClient
import helios.messages.DataMessages.PublishMAVLink
import helios.messages.CoreMessages.Unregistered
import helios.types.ClientTypes.FlightController
import helios.core.clients.DataMessages.WriteMAVLink
import helios.messages.CoreMessages.RegisterAPIClient
import akka.actor.OneForOneStrategy
import helios.types.ClientTypes.GroundControl
import helios.messages.CoreMessages.Registered
import helios.types.ClientTypes.API
import helios.messages.CoreMessages.SetSubscribers

object ClientReceptionist {
  val restartStrategy = OneForOneStrategy(maxNrOfRetries = 100,
    withinTimeRange = 1 seconds,
    loggingEnabled = false) {
    case _: Exception => Restart
  }

  def props(clientInfo: Iterable[ActorRefFactory => ActorRef], supervisorStrategy: SupervisorStrategy = restartStrategy): Props =
    Props(new ClientReceptionist(clientInfo, supervisorStrategy))
}

class ClientReceptionist(clientInfo: Iterable[ActorRefFactory => ActorRef], override val supervisorStrategy: SupervisorStrategy) extends Actor {

  val logger = LoggerFactory.getLogger(classOf[ClientReceptionist])

  val clients = clientInfo.map(a => context watch a(context))

  def subscriptionFilter(clientType: ClientType, clients: Subscribers): Subscribers = clientType match {
    case API(_) => clients.filter {
      case FlightController(_) => true
      case GenericSerialPort(_) => true
      case MAVLinkSerialPort(_) => true
      case _ => false
    }

    case FlightController(_) => clients.filter {
      case API(_) => true
      case GroundControl(_) => true
      case _ => false
    }

    case GroundControl(_) => clients.filter {
      case FlightController(_) => true
      case _ => false
    }

    case MAVLinkSerialPort(_) => clients.filter {
      case API(_) => true
      case _ => false
    }

    case GenericSerialPort(_) => clients.filter {
      case API(_) => true
      case _ => false
    }
  }

  def updateSubscriptions(activeClients: Subscribers) {
    activeClients foreach {
      c =>
        c.client ! SetSubscribers(subscriptionFilter(c, activeClients))
    }
  }

  override def preStart() = {
    logger.debug(context.self.path.toString)
  }

  def receive = defaultReceive()

  //TODO: uart ! SetPrimary ???
  def defaultReceive(activeClients: Subscribers = Set.empty): Receive = {
    case RegisterClient(ct) =>
      val ac = activeClients + ct
      context become defaultReceive(ac)
      sender ! Registered(ct)
      updateSubscriptions(ac)

    case UnregisterClient(ct) if activeClients contains ct =>
      context become defaultReceive(activeClients - ct)
      sender ! Unregistered()

    case RegisterAPIClient(ct) =>
      val hd: HeliosAPI =
        TypedActor(context.system)
          .typedActorOf(TypedProps(
          classOf[HeliosAPI], new HeliosAPIDefault("HeliosDefault", self, ct.client, 20)))

      val ac = activeClients + API(TypedActor(context.system).getActorRefFor(hd))
      context become defaultReceive(ac)

      sender ! hd
      updateSubscriptions(ac)

    case m@PublishMAVLink(ml) =>
      logger.error("ClientReceptionist should not receive PublishMAVLink messages!!")

    case WriteMAVLink(m) =>
      logger.error("ClientReceptionist should not receive WriteMAVLink messages!!")
  }
}