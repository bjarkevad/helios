package helios.core

import akka.actor._
import org.slf4j.LoggerFactory
import akka.actor.SupervisorStrategy._
import scala.concurrent.duration._
import scala.language.postfixOps
import helios.messages.CoreMessages.{Registered, UnregisterClient, RegisterClient, SetSubscribers}
import com.github.jodersky.flow.{PortInUseException, NoSuchPortException}

object ClientSupervisor {
  val defaultStrategy =
    OneForOneStrategy(maxNrOfRetries = 100,
      withinTimeRange = 1 seconds,
      loggingEnabled = true) {
      case _: java.io.IOException => Restart
      case _: PortInUseException => Restart
      case _: NoSuchPortException => Stop
      case _: NotImplementedError => Resume
      case _: Exception => Restart
    }

  def props(clientFactory: ActorRefFactory => ActorRef): Props =
    Props(new ClientSupervisor(clientFactory, defaultStrategy))

  def props(clientFactory: ActorRefFactory => ActorRef, supervisorStrategy: SupervisorStrategy): Props =
    Props(new ClientSupervisor(clientFactory, supervisorStrategy))
}

class ClientSupervisor(clientFactory: ActorRefFactory => ActorRef, override val supervisorStrategy: SupervisorStrategy) extends Actor {

  lazy val logger = LoggerFactory.getLogger(classOf[ClientSupervisor])

  val client: ActorRef = context watch clientFactory(context)

  override def postStop() = {
    client ! PoisonPill
  }

  //TODO: Forward based on message type
  def receive: Actor.Receive = {
    case m@RegisterClient(_) =>
      context.parent forward m

    case m@UnregisterClient(_) =>
      context.parent forward m

    case m@Registered(_) =>
      client forward m

    case m@SetSubscribers(_) =>
      client forward m

    case m@_ => println(s"Received: $m")
  }
}