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
  //TODO: fix supervision strategy for UARTs
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

class ClientReceptionist(clientProps: Iterable[(Props, String)], override val supervisorStrategy: SupervisorStrategy) extends Actor {

  val logger = LoggerFactory.getLogger(classOf[ClientReceptionist])

  val clients = clientProps.map(p => context watch context.actorOf(p._1, p._2))

  def flightcontrollers(clients: Set[ClientType] = Set.empty) = {
    clients
  }

  def updateSubscriptions(activeClients: Subscribers) {
    activeClients foreach(c => c.client ! SetSubscribers(activeClients.filter(!_.equals(c))))
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