package helios.api
import akka.actor.Actor
import akka.pattern.ask

import helios.api.HeliosAPI
import helios.apimessages.CoreMessages.RegisterAPIClient

import scala.concurrent.Await

class HeliosApplication extends Actor {
  lazy val Helios: HeliosAPI = {
    import scala.concurrent.duration._

    val clientRecep =
      context.actorSelection("akka.tcp://Main@localhost:2552/user/app")

    val f = ask(clientRecep, RegisterAPIClient(self))(3 seconds).mapTo[HeliosAPI]

    Await.result(f, 4 seconds)
  }

  override def preStart() = {

  }

  override def receive: Actor.Receive = {
    case _ =>
  }
}

