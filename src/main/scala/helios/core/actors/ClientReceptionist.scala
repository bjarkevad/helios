package helios.core.actors

import akka.actor.{Terminated, Props, ActorRef, Actor}
import helios.apimessages.CoreMessages._
import scala.collection.mutable
import helios.apimessages.CoreMessages.RegisterClient
import akka.actor.Terminated
import helios.apimessages.CoreMessages.UnregisterClient
import helios.apimessages.CoreMessages.Unregistered
import helios.core.actors.ClientHandler.{BecomeSecondary, BecomePrimary}
import scala.concurrent.ExecutionContext.Implicits.global

class ClientReceptionist extends Actor {

  /**Contains a map from ClientHandler to client */
  var clients: mutable.HashMap[ActorRef, ActorRef] = mutable.HashMap.empty

  override def preStart() = {
    context.actorOf(GroundControl())
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
        case None => sender ! NotRegistered(c)
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
