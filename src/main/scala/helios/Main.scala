package helios

object Main extends App {
  import akka.actor.ActorSystem
  import helios.core.actors.ClientReceptionist

  ActorSystem("Main").actorOf(ClientReceptionist.props)
}
