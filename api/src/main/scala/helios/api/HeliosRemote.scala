package helios.api

import akka.actor._
import akka.pattern.ask
import scala.concurrent.Await
import helios.api.HeliosAPI._
import helios.api.HeliosAPI.SystemStatus
import scala.util.{Success, Failure, Try}
import helios.messages.DataMessages.{UARTData, PublishMAVLink}
import helios.messages.CoreMessages.RegisterAPIClient
import helios.types.ClientTypes.API

class HeliosRemote(clientReceptionist: ActorRef) extends HeliosApplication
with TypedActor.Receiver
with TypedActor.PreStart
with TypedActor.PostStop {

  lazy val Helios: HeliosAPI = {
    import scala.concurrent.duration._
    import scala.language.postfixOps

    def loop: HeliosAPI = {
      Try(Await.result(
        ask(clientReceptionist, RegisterAPIClient(API(TypedActor.context.self)))(3 seconds).mapTo[HeliosAPI],
        4 seconds
      ))
      match {
        case Success(v: HeliosAPI) =>
          println(s"connected to core $v")
          v
        case Failure(e: Throwable) =>
          println(s"failed to connect to core: $e, retrying")
          loop
      }
    }
    loop
  }

  override def preStart() = {
    Helios.ping(0)
  }

  override def postStop() = {
    Helios.terminate()
    TypedActor.context.system.shutdown()
  }

  override def onReceive(message: Any, sender: ActorRef): Unit = {
    import org.mavlink.messages.MAV_STATE
    import Handlers._

    message match {
      case PublishMAVLink(m) =>
        if(m.componentId == 1)
          Streams.gcMlStream onNext m
        else
          Streams.fcMlStream onNext m

      case m: SystemStatus =>
        m.status match {
          case MAV_STATE.MAV_STATE_EMERGENCY =>
            emergencyHandler()
          case MAV_STATE.MAV_STATE_CRITICAL =>
            criticalHandler()
          case _ =>
        }
        Streams.statusStream onNext m

      case m: SystemPosition =>
        Streams.posStream onNext m

      case m: AttitudeRad =>
        Streams.attStream onNext m

      case UARTData(data) =>
        Streams.uStream onNext data

      case _ =>
    }
  }

  override val scheduler = TypedActor.context.system.scheduler
}

object HeliosRemote {
  def apply(apiHost: String, apiPort: Int): HeliosApplication = {
    val uri = s"akka.tcp://Main@$apiHost:$apiPort/user/receptionist"
    val as = ActorSystem("HeliosAPI")
    val clientRecep = TypedActor(as).system.provider.resolveActorRef(uri)
    TypedActor(as).typedActorOf(
      TypedProps(classOf[HeliosApplication], new HeliosRemote(clientRecep)))
  }

  def apply(): HeliosApplication = {
    val defaultUri = "akka.tcp://Main@localhost:2552/user/receptionist"
    val as = ActorSystem("HeliosAPI")
    val clientRecep = TypedActor(as).system.provider.resolveActorRef(defaultUri)
    TypedActor(as).typedActorOf(
      TypedProps(classOf[HeliosApplication], new HeliosRemote(clientRecep)))
  }

  //TODO: Used by tests, reconsider how this is done
  def apply(system: ActorSystem, receptionistPath: ActorPath): HeliosApplication = {
    val clientRecep = TypedActor(system).system.provider.resolveActorRef(receptionistPath)
    TypedActor(system).typedActorOf(
      TypedProps(classOf[HeliosApplication], new HeliosRemote(clientRecep)))
  }
}