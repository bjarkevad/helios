package helios.types

import akka.actor.ActorRef
import scala.reflect.ClassTag

object ClientTypes {
  trait ClientType {
    val client: ActorRef
  }
  case class Generic(client: ActorRef) extends ClientType
  case class FlightController(client: ActorRef) extends ClientType
  case class GroundControl(client: ActorRef) extends ClientType
  case class SerialPort(client: ActorRef) extends ClientType
  case class API(client: ActorRef) extends ClientType

  implicit class clientsImpls[B](clients: Iterable[B]) {
    def filterTypes[T <: B: ClassTag]: Iterable[B] = {
      clients.filter {
        case x: T => true
        case _ => false
      }
    }
  }
}
