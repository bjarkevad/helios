package helios.core.actors

import akka.actor.{ActorRef, Actor, Props}
import helios.core.flightcontroller.{MAVLinkMessage, FCComm}
import scala.util.{Success, Failure, Try}

case class MessageRead(data: MAVLinkMessage)

//TODO: Determine data type

case class ReadError(t: Throwable)

case class Write(data: MAVLinkMessage)

//TODO: Determine data type

case class Close()

case class Subscribe(actor: ActorRef)

case class Unsubscribe(actor: ActorRef)


//TODO: Decide if FC should be injected or not.. It probably should..n't..?
class BeagleBoneReader(val flightController: FCComm) extends Actor {

  private var subscribers = Set.empty[ActorRef]
  private val subscribersLock = new Object

  private def broadcast(msg: Any) = subscribersLock synchronized {
    subscribers foreach (_ ! msg)
  }

  private object Reader extends Thread {
    def readLoop() = {
      var running = true
      while (running) {
        flightController.read match {
          case Success(m) =>
            self ! MessageRead(m)
          case Failure(e) =>
            running = false
            self ! ReadError(e)
        }
      }
    }

    override def run() {
      flightController.open
      readLoop()
    }
  }

  override def preStart() = {
    Reader.start()
  }

  override def postStop() = {
    flightController.close
  }

  def receive: Receive = {
    case MessageRead(data) => broadcast(data)

    case ReadError(e) => throw e //Let it crash!

    case Write(data) => flightController.write(data)

    case Subscribe(actor) => subscribersLock synchronized {
      subscribers += actor
    }

    case Unsubscribe(actor) => subscribersLock synchronized {
      subscribers -= actor
    }
  }
}
