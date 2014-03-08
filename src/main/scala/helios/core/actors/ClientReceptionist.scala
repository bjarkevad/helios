package helios.core.actors

import akka.actor.{Terminated, Props, ActorRef, Actor}
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global

import helios.apimessages.CoreMessages._
import helios.core.actors.ClientHandler.{BecomeSecondary, BecomePrimary}

import org.slf4j.LoggerFactory
import akka.io.{IO, UdpConnected}

class ClientReceptionist extends Actor {

  /**Contains a map from ClientHandler to Client */
  var clients: mutable.HashMap[ActorRef, ActorRef] = mutable.HashMap.empty
  val logger = LoggerFactory.getLogger(classOf[ClientReceptionist])

  override def preStart() = {
    import context.system
    context.actorOf(GroundControl.props(IO(UdpConnected)))
    logger.debug("Started")
  }

  override def postStop() = {

  }

  def receive: Receive = {
    case RegisterClient(c) =>
      val ch = context.actorOf(ClientHandler(c, self))

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
