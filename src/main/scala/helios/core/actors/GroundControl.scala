package helios.core.actors

import akka.actor.{ActorRef, Actor}
import java.net.{InetAddress, DatagramPacket, DatagramSocket}
import org.mavlink.messages._
import helios.core.actors.GroundControl.Register
import org.mavlink.messages.common.msg_heartbeat

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
    val socket = new DatagramSocket() //Throws on failure
    val a = InetAddress.getByName("localhost")
//    val b = Array(192.toByte,168.toByte,7.toByte,2.toByte)
//    val a = InetAddress.getByAddress(b)
    val rxPacket = new DatagramPacket(buffer, bufSize, a, port)
    val txPacket = new DatagramPacket(buffer, bufSize, a, port)

    val hb = new msg_heartbeat(20, MAV_COMPONENT.MAV_COMP_ID_IMU)
    hb.sequence = 0
    hb.`type` = MAV_TYPE.MAV_TYPE_QUADROTOR
    hb.autopilot = MAV_AUTOPILOT.MAV_AUTOPILOT_GENERIC
    hb.base_mode = MAV_MODE_FLAG.MAV_MODE_FLAG_AUTO_ENABLED //MAV_MODE.MAV_MODE_PREFLIGHT
    hb.custom_mode = 0
    hb.system_status = MAV_STATE.MAV_STATE_STANDBY
    hb.mavlink_version = 3

    val msg = hb.encode()

    override def run() = {
      var running = true

      while (running) {
        try {
          txPacket.setData(msg)
          socket.send(txPacket)
          Thread.sleep(1000)
          //socket.receive(rxPacket)
        } catch {
          case e: Throwable =>
            running = false
            throw e // Let it crash
        }

        //self ! MessageRead(rxPacket.getData)
      }
    }
  }

  override def preStart() = {
    udpReader.start()
  }

  override def postStop() = {
    udpReader.socket.close()
  }

  import helios.util.ByteConverter._

  override def receive: Actor.Receive = {
    case msg@GroundControl.MessageRead(v) =>
      println(s"received: ${v.byteArrayToHex}")
      rec ! msg

    case Register(actor) => println("register!")
    case _ => println("received something unexpected")
  }

}
