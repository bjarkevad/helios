package helios.core

import akka.actor._
import org.slf4j.LoggerFactory
import akka.actor.SupervisorStrategy._
import scala.concurrent.duration._
import scala.language.postfixOps
import helios.messages.CoreMessages.SetSubscribers

object ClientSupervisor {
  def props(clientFactory: ActorRefFactory => ActorRef): Props =
    Props(new ClientSupervisor(clientFactory))
}

class ClientSupervisor(clientFactory: ActorRefFactory => ActorRef) extends Actor {

  lazy val logger = LoggerFactory.getLogger(classOf[ClientSupervisor])

  val client: ActorRef = context watch clientFactory(context)

  override val supervisorStrategy = OneForOneStrategy(5, 5.seconds) {
    case _: Exception => Restart
  }

  override def postStop() = {
    client ! PoisonPill
  }

  def receive: Actor.Receive = {
    case m@SetSubscribers(_) =>
      client forward m

    case m@_ => println(s"Received: $m")
  }
}