package helios.core.actors

import akka.actor.{Props, ActorRef, Actor}
import java.net.{InetAddress, DatagramPacket, DatagramSocket}
import org.mavlink.messages._
import org.mavlink.messages.common.msg_heartbeat

import GroundControl._

object GroundControl {
  case class UdpMsgRead(data: Array[Byte])
  case class Register(actor: ActorRef)

  def apply(): Props = Props(classOf[GroundControl])
}
class GroundControl extends Actor {

  private object udpReader extends Thread {
    //TODO: inject values
    val port = 14550
    val bufSize = 16
    //TODO: Figure size out dynamically? Read the header first, then determine the packet size from there
    val buffer = new Array[Byte](bufSize)
    val socket = new DatagramSocket() //Throws on failure
    val a = InetAddress.getByName("localhost")

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

        self ! UdpMsgRead(rxPacket.getData)
      }
    }
  }

  override def preStart() = {
    udpReader.start()
    //schedule heartbeat
  }

  override def postStop() = {
    udpReader.socket.close()
  }

  import helios.util.ByteConverter._

  override def receive: Actor.Receive = {
    case msg@UdpMsgRead(v) =>
      println(s"received: ${v.byteArrayToHex}")
      //val m = convertToMAVLink(msg)
      //handler ! m
      context.parent ! msg

    case Register(actor) => println("register!")
    case _ => println("received something unexpected")
  }

}
