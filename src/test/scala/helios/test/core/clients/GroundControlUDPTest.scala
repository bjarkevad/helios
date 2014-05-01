package helios.test.core.clients

import org.scalatest._
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestProbe, TestKit}
import scala.concurrent.duration._

import scala.language.postfixOps
import akka.io.UdpConnected
import java.net.InetSocketAddress
import akka.util.ByteString
import org.mavlink.messages.common.msg_heartbeat
import org.mavlink.messages._
import helios.messages.DataMessages.PublishMAVLink
import helios.messages.CoreMessages.{Registered, ClientType, GroundControl}
import helios.core.clients.GroundControlUDP

class GroundControlUDPTest extends TestKit(ActorSystem("GroundControlTest"))
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

  def ctf(a: ActorRef): ClientType = GroundControl(a)

  lazy val address: InetSocketAddress = {
    new InetSocketAddress("localhost", 14550)
  }

  override def afterAll() = {
    system.shutdown()
  }

  ignore should "send a single heartbeat on connected" in {
    val udpMan = TestProbe()
    val udpCon = TestProbe()

    val gc = system.actorOf(GroundControlUDP.props(ctf, udpMan.ref, address))

    udpMan.expectMsg(UdpConnected.Connect(gc, new InetSocketAddress("localhost", 14550)))
    udpCon.send(gc, UdpConnected.Connected)
    udpCon.expectMsgClass(classOf[UdpConnected.Send])
  }

  ignore should "stash and send MAVLink helios.api.messages to its handler" in {
    val udpMan = TestProbe()
    val udpCon = TestProbe()

    val gc = system.actorOf(GroundControlUDP.props(ctf, udpMan.ref, address))

    udpMan.expectMsg(UdpConnected.Connect(gc, new InetSocketAddress("localhost", 14550)))
    udpCon.send(gc, UdpConnected.Connected)
    udpCon.expectMsgClass(classOf[UdpConnected.Send]) //HB

    udpCon.send(gc, UdpConnected.Received(ByteString(heartbeat(0).encode()))) //Stashed

    udpMan.send(gc, Registered(ctf(gc)))
    udpMan.expectMsgClass(classOf[PublishMAVLink]) //Stashed should be replayed

    udpCon.send(gc, UdpConnected.Received(ByteString(heartbeat(1).encode()))) //normal

    udpMan.expectMsgClass(classOf[PublishMAVLink]) //normal should be sent to handler
  }

  ignore should "not send unknown helios.api.messages to its handler" in {
    val udpMan = TestProbe()
    val udpCon = TestProbe()

    val gc = system.actorOf(GroundControlUDP.props(ctf, udpMan.ref, address))

    udpMan.expectMsg(UdpConnected.Connect(gc, new InetSocketAddress("localhost", 14550)))
    udpCon.send(gc, UdpConnected.Connected)
    udpCon.expectMsgClass(classOf[UdpConnected.Send]) //HB

    udpCon.send(gc, UdpConnected.Received(ByteString("Trigger registration"))) //normal

    udpMan.send(gc, Registered(ctf(gc)))

    udpCon.send(gc, UdpConnected.Received(ByteString("Unknown message"))) //normal

    udpMan.expectNoMsg(1 second)
  }
}
