package helios.api

import akka.actor.Actor
import akka.pattern.ask

import helios.api.HeliosAPI._
import helios.apimessages.CoreMessages.RegisterAPIClient
import rx.lang.scala.Observable
import rx.lang.scala.Subject

import scala.concurrent.Await


class HeliosApplication extends Actor {
  Helios.ping(0)

  lazy val statusStream: Subject[SystemStatus] = Subject()
  lazy val locStream: Subject[Location] = Subject()

  lazy val Helios: HeliosAPI = {
    import scala.concurrent.duration._
    import scala.language.postfixOps

    val clientRecep = //TODO: Figure out a way to configure where the remote receptionist is
      context.actorSelection("akka.tcp://Main@localhost:2552/user/app")

    //RegisterAPIClient returns a typed actor
    val f = ask(clientRecep, RegisterAPIClient(self))(3 seconds).mapTo[HeliosAPI]

    Await.result(f, 4 seconds)
  }

  override def preStart() = {
    Helios.ping(0)
  }

  override def postStop() = {
    Helios.terminate()
  }

  override def receive: Actor.Receive = {
    case m@SystemStatus(_,_,_,_,_) =>
      statusStream.onNext(m)
//    case m@Location(_) =>
//      locStream.onNext(m)
    case _ =>
  }

  implicit class HeliosAPICompanion(val helios: HeliosAPI) {
    lazy val systemStatusStream: Observable[SystemStatus] = statusStream
    lazy val locationStream: Observable[Location] = locStream
  }
}

