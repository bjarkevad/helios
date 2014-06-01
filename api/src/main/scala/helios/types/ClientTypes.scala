package helios.types

import akka.actor.ActorRef
import scala.reflect.ClassTag

object ClientTypes {
  trait ClientType {
    val client: ActorRef
  }

  case class FlightController(client: ActorRef) extends ClientType
  case class GroundControl(client: ActorRef) extends ClientType
  case class GenericSerialPort(client: ActorRef) extends ClientType
  case class MAVLinkSerialPort(client: ActorRef) extends ClientType
  case class API(client: ActorRef) extends ClientType

  /**
   * Adds a filterTypes to Iterables
   * @param clients the iterable to filter
   * @tparam B The type of the data in the iterable
   */
  implicit class clientsImpls[B](clients: Iterable[B]) {
    def filterTypes[T <: B: ClassTag]: Iterable[B] = {
      clients.filter {
        case x: T => true
        case _ => false
      }
    }
  }
}
