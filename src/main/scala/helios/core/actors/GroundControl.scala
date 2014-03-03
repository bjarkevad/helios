package helios.core.actors

import akka.actor._
import java.net.{InetAddress, DatagramPacket, DatagramSocket}
import org.mavlink.messages._
import org.mavlink.messages.common.msg_heartbeat

import scala.collection.mutable.Buffer
import helios.core.actors.GroundControl.UdpPacketRead

object GroundControl {
  case class UdpPacketRead(data: Array[Byte])

  def apply(): Props = Props(classOf[GroundControl])
}
class GroundControl extends Actor {
  import helios.apimessages.CoreMessages._
  import concurrent.duration._
  import language.postfixOps
  import scala.concurrent.ExecutionContext.Implicits.global

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

        self ! UdpPacketRead(rxPacket.getData)
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

  val packetCache: Buffer[UdpPacketRead] = Buffer.empty

  def receive: Actor.Receive = unregistered orElse failure



  def unregistered: Actor.Receive = {
    case msg@UdpPacketRead(v) =>
      val c = context.system.scheduler.schedule(0 millis, 100 millis, context.parent, RegisterClient(self))
      context become awaitingRegistration(c).orElse(failure)
      self ! msg
  }

  def awaitingRegistration(registerTask: Cancellable): Actor.Receive = {
    case msg@UdpPacketRead(v) =>
      packetCache append msg

    case Registered(c) if c == self =>
      registerTask.cancel()
      context become registered(sender).orElse(failure)
      packetCache foreach(self ! _)
      packetCache clear()
  }

  def registered(handler: ActorRef): Actor.Receive = {
    case msg@UdpPacketRead(v) =>
      //convertToMAVLink(v)
  }

  def failure: Actor.Receive = {
    case m@_ => println(s"GroundControl received something unexpected: $m")
  }
}
