package helios.api

import akka.actor._
import akka.pattern.ask
import rx.lang.scala.Observable
import rx.lang.scala.Subject
import scala.concurrent.Await
import helios.api.HeliosAPI._
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

    val clientRecep = TypedActor.context.actorSelection(apiUri)

    //RegisterAPIClient returns a typed actor
    Await.result(
      ask(clientRecep, RegisterAPIClient(TypedActor.context.self))(10 seconds).mapTo[HeliosAPI],
      11 seconds
    )
  }

  override def preStart() = {
    Helios.ping(0)
  }

  override def postStop() = {
    Helios.terminate()
  }

  override def onReceive(message: Any, sender: ActorRef): Unit = {
    message match {
      case m: SystemStatus =>
        statusStream onNext m

      case m: SystemLocation =>
        locStream onNext m

      case m: AttitudeRad =>
        attStream onNext m

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
  def apply(apiHost: String, apiPort: Int): HeliosApplication = {
    val uri = s"akka.tcp://Main@$apiHost:$apiPort/user/*"
    val as = ActorSystem("HeliosAPI")
    TypedActor(as).typedActorOf(
      TypedProps(classOf[HeliosApplication], new HeliosApplicationDefault(uri)))
  }

  def apply(): HeliosApplication = {
    val defaultUri = "akka.tcp://Main@localhost:2552/user/*"
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

  private[api]
  lazy val attStream: Subject[AttitudeRad] = Subject()

  implicit class HeliosAPIImp(val helios: HeliosAPI) {
    lazy val systemStatusStream: Observable[SystemStatus] = statusStream
    lazy val locationStream: Observable[SystemLocation] = locStream
    lazy val attitudeRadStream: Observable[AttitudeRad] = attStream
    lazy val attitudeDegStream: Observable[AttitudeDeg] = attStream map {
      a =>
        println("AttitudeDeg Called..")
        AttitudeDeg(
          Math.toDegrees(a.roll).toFloat,
          Math.toDegrees(a.pitch).toFloat,
          Math.toDegrees(a.yaw).toFloat
        )
    }
  }

}