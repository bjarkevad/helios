package helios.api

import akka.actor._
import akka.pattern.ask
import scala.concurrent.Await
import helios.api.HeliosAPI._
import helios.api.HeliosAPI.SystemStatus
import scala.language.postfixOps
import helios.messages.DataMessages.{UartData, PublishMAVLink}
import helios.messages.CoreMessages.{API, RegisterAPIClient}
import scala.concurrent.duration._

class HeliosLocal(clientReceptionist: ActorRef) extends HeliosApplication
with TypedActor.Receiver
with TypedActor.PreStart
with TypedActor.PostStop {

  import Streams._

  override lazy val Helios: HeliosAPI = {
    Await.result(
      ask(clientReceptionist, RegisterAPIClient(API(TypedActor.context.self)))(3 seconds).mapTo[HeliosAPI],
      4 seconds
    )
  }

  override val scheduler = TypedActor.context.system.scheduler

  override def preStart(): Unit = {
    println("Starting")
  }

  override def postStop(): Unit = {
    Helios.terminate()
    TypedActor.context.system.shutdown()
  }

  override def onReceive(message: Any, sender: ActorRef): Unit = {
    import org.mavlink.messages.MAV_STATE
    import Handlers._

    message match {
      case PublishMAVLink(m) =>
        if(m.componentId == 1)
          gcMlStream onNext m
        else
          fcMlStream onNext m

      case m: SystemStatus =>
        m.status match {
          case MAV_STATE.MAV_STATE_EMERGENCY =>
            emergencyHandler()
          case MAV_STATE.MAV_STATE_CRITICAL =>
            criticalHandler()
          case _ =>
        }
        statusStream onNext m

      case m: SystemPosition =>
        posStream onNext m

      case m: AttitudeRad =>
        attStream onNext m

      case UartData(data) =>
        uStream onNext data

      case _ =>
    }
  }
}

object HeliosLocal {

  import helios.Main.HeliosInit

  def apply(): HeliosApplication = {
    implicit val as = ActorSystem("HeliosAPI")
    val recep = HeliosInit
    TypedActor(as).typedActorOf(
      TypedProps(classOf[HeliosApplication], new HeliosLocal(recep)))
  }
}