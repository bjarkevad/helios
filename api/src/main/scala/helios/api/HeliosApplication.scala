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

class HeliosApplicationDefault(clientReceptionist: ActorRef) extends HeliosApplication
with TypedActor.Receiver
with TypedActor.PreStart
with TypedActor.PostStop {

  import Streams._

  lazy val Helios: HeliosAPI = {
    import scala.concurrent.duration._
    import scala.language.postfixOps


    //RegisterAPIClient returns a typed actor
    Await.result(
      ask(clientReceptionist, RegisterAPIClient(TypedActor.context.self))(10 seconds).mapTo[HeliosAPI],
      11 seconds
    )
  }

  override def preStart() = {
    Helios.ping(0)
  }

  override def postStop() = {
    Helios.terminate()
    TypedActor.context.system.shutdown()
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
    val uri = s"akka.tcp://Main@$apiHost:$apiPort/user/receptionist"
    val as = ActorSystem("HeliosAPI")
    val clientRecep = TypedActor(as).system.provider.resolveActorRef(uri)
    TypedActor(as).typedActorOf(
      TypedProps(classOf[HeliosApplication], new HeliosApplicationDefault(clientRecep)))
  }

  def apply(): HeliosApplication = {
    val defaultUri = "akka.tcp://Main@localhost:2552/user/receptionist"
    val as = ActorSystem("HeliosAPI")
    val clientRecep = TypedActor(as).system.provider.resolveActorRef(defaultUri)
    TypedActor(as).typedActorOf(
      TypedProps(classOf[HeliosApplication], new HeliosApplicationDefault(clientRecep)))
  }

  private[api]
  def apply(system: ActorSystem, path: ActorPath): HeliosApplication = {
    val clientRecep = TypedActor(system).system.provider.resolveActorRef(path)
    TypedActor(system).typedActorOf(
      TypedProps(classOf[HeliosApplication], new HeliosApplicationDefault(clientRecep)))
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
        AttitudeDeg(
          Math.toDegrees(a.roll).toFloat,
          Math.toDegrees(a.pitch).toFloat,
          Math.toDegrees(a.yaw).toFloat
        )
    }
  }
}