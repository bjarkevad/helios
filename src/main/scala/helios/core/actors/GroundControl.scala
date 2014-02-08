package helios.core.actors

import akka.actor.{ActorRef, Actor}
import java.net.{DatagramPacket, DatagramSocket}
import scala.util.{Success, Try}
import helios.core.actors.GroundControl.Register

object GroundControl {
  case class MessageRead(data: Array[Byte])
  case class Register(actor: ActorRef)
}
class GroundControl(val rec: ActorRef) extends Actor {

  import GroundControl.MessageRead

  private object udpReader extends Thread {
    //TODO: inject values
    val port = 14550
    val bufSize = 16
    //TODO: Figure size out dynamically? Read the header first, then determine the packet size from there
    val buffer = new Array[Byte](bufSize)
    val socket = new DatagramSocket(port) //Throws on failure
    val packet = new DatagramPacket(buffer, bufSize)


    override def run() = {
      var running = true

      while (running) {
        try {
          socket.receive(packet)
        } catch {
          case e: Throwable =>
            running = false
            throw e // Let it crash
        }

        self ! MessageRead(packet.getData)
      }
    }
  }

  override def preStart() = {
    udpReader.start()
  }

  override def postStop() = {
    udpReader.socket.close()
  }

  override def receive: Actor.Receive = {
    case msg@GroundControl.MessageRead(v) =>
      println(s"received: $v")
      rec ! msg

    case Register(actor) => println("register!")
    case _ => println("received something unexpected")
  }

}
