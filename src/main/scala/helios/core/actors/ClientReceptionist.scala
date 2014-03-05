package helios.core.actors

import akka.actor.{Terminated, Props, ActorRef, Actor}
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global

import helios.apimessages.CoreMessages._
import helios.core.actors.ClientHandler.{BecomeSecondary, BecomePrimary}

import org.slf4j.LoggerFactory

class ClientReceptionist extends Actor {

  /**Contains a map from ClientHandler to Client */
  var clients: mutable.HashMap[ActorRef, ActorRef] = mutable.HashMap.empty
  val logger = LoggerFactory.getLogger(classOf[ClientReceptionist])

  override def preStart() = {
    context.actorOf(GroundControl())
    logger.debug("Started")
  }

  override def postStop() = {

  }

  def receive: Actor.Receive = {
    case RegisterClient(c) =>
      val ch = context.actorOf(Props(new ClientHandler(c)))

      //First clienthandler is primary
      if(clients.isEmpty) {
        ch ! BecomePrimary()
      }

      clients put (ch, c) match {
        case None =>
        case Some(_) =>
      }

    case UnregisterClient(c) =>
      clients remove c
      sender ! Unregistered(c)

    case Terminated(a) =>
      if(clients contains a) {
        val client = clients(a)
        client ! Unregistered(client)
      }

    case _ => sender ! NotRegistered(sender)
  }
}
