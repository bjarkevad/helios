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
import helios.types.ClientTypes.ClientType

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

  def flightcontrollers(clients: Set[ClientType] = Set.empty) = {
    clients
  }

  def updateSubscriptions(activeClients: Subscribers) {
    activeClients foreach (c => c.client ! SetSubscribers(activeClients.filter(!_.equals(c))))
  }

  override def preStart() = {
    logger.debug(context.self.path.toString)
  }

  def receive = defaultReceive()

  //TODO: uart ! SetPrimary ???
  def defaultReceive(activeClients: Set[ClientType] = Set.empty): Receive = {
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

      val ac = activeClients + ct
      context become defaultReceive(ac)

      sender ! hd
      updateSubscriptions(ac)

    case m@PublishMAVLink(ml) =>
      logger.error("ClientReceptionist should not receive PublishMAVLink messages!!")

    case WriteMAVLink(m) =>
      logger.error("ClientReceptionist should not receive WriteMAVLink messages!!")
  }
}