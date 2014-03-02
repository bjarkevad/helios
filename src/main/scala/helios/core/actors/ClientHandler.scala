package helios.core.actors

import akka.actor.{ActorRef, Actor}

class ClientHandler(client: ActorRef) extends Actor {
  def receive: Actor.Receive = ???
}
