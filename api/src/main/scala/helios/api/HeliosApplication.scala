package helios.api

import akka.actor._
import akka.pattern.ask
import rx.lang.scala.Observable
import rx.lang.scala.Subject
import scala.concurrent.Await
import helios.api.HeliosAPI._
import helios.api.HeliosAPI.SystemStatus
import helios.api.HeliosApplicationDefault.{UartData, RegisterAPIClient}
import akka.util.ByteString
import scala.util.{Success, Failure, Try}

object HeliosApplicationDefault {

  case class RegisterAPIClient(client: ActorRef)

  case class UnregisterAPIClient(client: HeliosAPI)

  case class UartData(data: ByteString)
}

class HeliosApplicationDefault(clientReceptionist: ActorRef) extends HeliosApplication
with TypedActor.Receiver
with TypedActor.PreStart
with TypedActor.PostStop {

  import Streams._

  lazy val Helios: HeliosAPI = {
    import scala.concurrent.duration._
    import scala.language.postfixOps

    def loop: HeliosAPI = {
      Try(Await.result(
        ask(clientReceptionist, RegisterAPIClient(TypedActor.context.self))(3 seconds).mapTo[HeliosAPI],
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

  //TODO: Used by tests, reconsider how this is done
  private[api]
  def apply(system: ActorSystem, path: ActorPath): HeliosApplication = {
    val clientRecep = TypedActor(system).system.provider.resolveActorRef(path)
    TypedActor(system).typedActorOf(
      TypedProps(classOf[HeliosApplication], new HeliosApplicationDefault(clientRecep)))
  }
}

//TODO: Use BehaviorSubjects to cache the latest message for new subscribers
object Streams {
  private[api]
  lazy val statusStream: Subject[SystemStatus] = Subject()

  private[api]
  lazy val posStream: Subject[SystemPosition] = Subject()

  private[api]
  lazy val attStream: Subject[AttitudeRad] = Subject()

  private [api]
  lazy val uStream: Subject[ByteString] = Subject()

  implicit class StreamsImp(val helios: HeliosAPI) {
    lazy val systemStatusStream: Observable[SystemStatus] = statusStream
    lazy val positionStream: Observable[SystemPosition] = posStream
    lazy val attitudeRadStream: Observable[AttitudeRad] = attStream
    lazy val attitudeDegStream: Observable[AttitudeDeg] = attStream map {
      a =>
        AttitudeDeg(
          Math.toDegrees(a.roll).toFloat,
          Math.toDegrees(a.pitch).toFloat,
          Math.toDegrees(a.yaw).toFloat
        )
    }

    lazy val uartStream: Observable[ByteString] = uStream
  }
}

object Handlers {
  private[api]
  var criticalHandler: () => Unit =
    () => println("System entered critical mode with no handler!")

  var emergencyHandler: () => Unit =
    () => println("System entered emergency mode with no handler!")

  implicit class HandlersImp(val helios: HeliosAPI) {
    def setCriticalHandler(f: () => Unit): Unit =
      criticalHandler = f

    def setEmergencyHandler(f: () => Unit): Unit =
      emergencyHandler = f
  }
}