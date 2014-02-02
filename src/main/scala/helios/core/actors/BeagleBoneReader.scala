package helios.core.actors

import akka.actor.{ActorRef, Actor, Props}
import helios.core.flightcontroller.FCComm
import scala.util.Try

case class LineRead(data: String)

case class ReadError(t: Throwable)

case class Close()

case class Subscribe(actor: ActorRef)

case class Unsubscribe(actor: ActorRef)


class BeagleBoneReader(val flightController: FCComm) extends Actor {

  private var subscribers = Set.empty[ActorRef]
  private val subscribersLock = new Object

  private def broadcast(msg: AnyRef) = subscribersLock synchronized {
    subscribers foreach (_ ! msg)
  }

  private object Reader extends Thread {
    def readLoop() = {
      var running = true
      while (running) {
        Try(flightController.read[String]).map {
          self ! LineRead(_)
        } recover {
          case e@_ =>
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
    case LineRead(data) => broadcast(data)

    case ReadError(e) => throw e //Let it crash!

    case Subscribe(actor) => subscribersLock synchronized {
      subscribers += actor
    }

    case Unsubscribe(actor) => subscribersLock synchronized {
      subscribers -= actor
    }
  }
}
