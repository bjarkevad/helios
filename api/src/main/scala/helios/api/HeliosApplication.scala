package helios.api

import akka.actor._
import akka.pattern.ask

import helios.api.HeliosAPI._
import rx.lang.scala.Observable
import rx.lang.scala.Subject

import scala.concurrent.Await
import helios.api.HeliosAPI.SystemStatus
import helios.api.HeliosApplicationDefault.RegisterAPIClient

object HeliosApplicationDefault {
  case class RegisterAPIClient(client: ActorRef)

  case class UnregisterAPIClient(client: HeliosAPI)
}

class HeliosApplicationDefault(apiUri: String) extends HeliosApplication
with TypedActor.Receiver
with TypedActor.PreStart
with TypedActor.PostStop {
  import Streams._

  lazy val Helios: HeliosAPI = {
    import scala.concurrent.duration._
    import scala.language.postfixOps

    val clientRecep = //TODO: Figure out a way to configure where the remote receptionist is
      TypedActor.context.actorSelection("akka.tcp://Main@localhost:2552/user/app")

    //RegisterAPIClient returns a typed actor
    val f = ask(clientRecep, RegisterAPIClient(TypedActor.context.self))(3 seconds).mapTo[HeliosAPI]

    Await.result(f, 4 seconds)
  }

  override def preStart() = {
    Helios.ping(0)
  }

  override def postStop() = {
    Helios.terminate()
  }

  override def onReceive(message: Any, sender: ActorRef): Unit = {
    message match {
      case m@SystemStatus(_, _, _, _, _) =>
        statusStream.onNext(m)

      case m@SystemLocation(_) =>
        locStream.onNext(m)

      case _ =>
    }
  }

  override val scheduler = TypedActor.context.system.scheduler
}

trait HeliosApplication {
  val Helios: HeliosAPI
  val scheduler: Scheduler
}

object HeliosApplication {
  def apply(apiHost: String, apiPort: String): HeliosApplication = {
    val uri = s"akka.tcp://Main@$apiHost:$apiPort/user/app"
    val as = ActorSystem("HeliosAPI")
    TypedActor(as).typedActorOf(
      TypedProps(classOf[HeliosApplication], new HeliosApplicationDefault(uri)))
  }

  def apply(): HeliosApplication = {
    val defaultUri = "akka.tcp://Main@localhost:2552/user/app"
    val as = ActorSystem("HeliosAPI")
    TypedActor(as).typedActorOf(
      TypedProps(classOf[HeliosApplication], new HeliosApplicationDefault(defaultUri)))
  }
}

object Streams {
  private[api]
  lazy val statusStream: Subject[SystemStatus] = Subject()

  private[api]
  lazy val locStream: Subject[SystemLocation] = Subject()

  implicit class HeliosAPIImp(val helios: HeliosAPI) {
    val systemStatusStream: Observable[SystemStatus] = statusStream
    val locationStream: Observable[SystemLocation] = locStream
  }

}
