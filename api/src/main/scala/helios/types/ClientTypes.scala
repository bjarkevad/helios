package helios.types

import akka.actor.ActorRef

object ClientTypes {
  trait ClientType {
    val client: ActorRef
  }
  case class Generic(client: ActorRef) extends ClientType
  case class FlightController(client: ActorRef) extends ClientType
  case class GroundControl(client: ActorRef) extends ClientType
  case class SerialPort(client: ActorRef) extends ClientType
  case class API(client: ActorRef) extends ClientType

  def filterGenerics[A <: Iterable[ClientType]](clients: A): Iterable[ClientType] = {
    clients.filter(_.isInstanceOf[Generic])
  }

  def filterFlightControllers[A <: Iterable[ClientType]](clients: A): Iterable[ClientType] = {
    clients.filter(_.isInstanceOf[FlightController])
  }

  def filterGroundControls[A <: Iterable[ClientType]](clients: A): Iterable[ClientType] = {
    clients.filter(_.isInstanceOf[GroundControl])
  }

  def filterSerialPorts[A <: Iterable[ClientType]](clients: A): Iterable[ClientType] = {
    clients.filter(_.isInstanceOf[SerialPort])
  }

  def filterAPIs[A <: Iterable[ClientType]](clients: A): Iterable[ClientType] = {
    clients.filter(_.isInstanceOf[API])
  }
}
