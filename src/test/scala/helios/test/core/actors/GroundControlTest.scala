package helios.test.core.actors

import org.scalatest._
import akka.actor.{Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestProbe, TestKit}
import helios.core.actors.GroundControl
import scala.concurrent.duration._

import scala.language.postfixOps
import scala.concurrent
import akka.io.UdpConnected
import java.net.InetSocketAddress
import akka.util.ByteString
import helios.apimessages.CoreMessages.{Registered, RegisterClient}
import org.mavlink.messages.common.msg_heartbeat
import org.mavlink.messages._
import helios.apimessages.CoreMessages.Registered
import helios.apimessages.MAVLinkMessages.RawMAVLink

class GroundControlTest extends TestKit(ActorSystem("GroundControlTest"))
with FlatSpecLike
with BeforeAndAfterAll
with ShouldMatchers
with ImplicitSender {

  def heartbeat(seq: Int): MAVLinkMessage = {
    val hb = new msg_heartbeat(20, MAV_COMPONENT.MAV_COMP_ID_IMU)
    hb.sequence = seq
    hb.`type` = MAV_TYPE.MAV_TYPE_QUADROTOR
    hb.autopilot = MAV_AUTOPILOT.MAV_AUTOPILOT_GENERIC
    hb.base_mode = MAV_MODE_FLAG.MAV_MODE_FLAG_AUTO_ENABLED //MAV_MODE.MAV_MODE_PREFLIGHT
    hb.custom_mode = 0
    hb.system_status = MAV_STATE.MAV_STATE_STANDBY
    hb.mavlink_version = 3

    hb
  }

  override def afterAll() = {
    system.shutdown()
  }

  "GroundControl" should "start sending heartbeats on connected" in {
    val udpMan = TestProbe()
    val udpCon = TestProbe()

    val gc = system.actorOf(GroundControl.props(udpMan.ref))

    udpMan.expectMsg(UdpConnected.Connect(gc, new InetSocketAddress("localhost", 14550)))
    udpCon.send(gc, UdpConnected.Connected)
    udpCon.expectMsgClass(classOf[UdpConnected.Send])
    udpCon.expectMsgClass(classOf[UdpConnected.Send])
    udpCon.expectMsgClass(classOf[UdpConnected.Send])
  }

  it should "stash and send MAVLink messages to its handler" in {
    val udpMan = TestProbe()
    val udpCon = TestProbe()

    val gc = system.actorOf(GroundControl.props(udpMan.ref))

    udpMan.expectMsg(UdpConnected.Connect(gc, new InetSocketAddress("localhost", 14550)))
    udpCon.send(gc, UdpConnected.Connected)
    udpCon.expectMsgClass(classOf[UdpConnected.Send]) //HB

    udpCon.send(gc, UdpConnected.Received(ByteString(heartbeat(0).encode()))) //Stashed

    udpMan.send(gc, Registered(gc))
    udpMan.expectMsgClass(classOf[RawMAVLink]) //Stashed should be replayed

    udpCon.send(gc, UdpConnected.Received(ByteString(heartbeat(1).encode()))) //normal

    udpMan.expectMsgClass(classOf[RawMAVLink]) //normal should be sent to handler
  }

  it should "not send unknown messages to its handler" in {
    val udpMan = TestProbe()
    val udpCon = TestProbe()

    val gc = system.actorOf(GroundControl.props(udpMan.ref))

    udpMan.expectMsg(UdpConnected.Connect(gc, new InetSocketAddress("localhost", 14550)))
    udpCon.send(gc, UdpConnected.Connected)
    udpCon.expectMsgClass(classOf[UdpConnected.Send]) //HB

    udpCon.send(gc, UdpConnected.Received(ByteString("Trigger registration"))) //normal

    udpMan.send(gc, Registered(gc))

    udpCon.send(gc, UdpConnected.Received(ByteString("Unknown message"))) //normal

    udpMan.expectNoMsg(1 second)
  }
}
