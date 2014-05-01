package helios.test.core.clients

import org.scalatest._

import com.github.jodersky.flow._
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import akka.actor._

import scala.language.postfixOps
import scala.concurrent.duration._

import akka.util.ByteString
import org.mavlink.messages._
import org.mavlink.messages.common._

import helios.util.Privileged.PrivilegedLike
import helios.core.clients.MAVLinkUART
import helios.messages.CoreMessages._
import helios.messages.CoreMessages.RegisterClient
import helios.messages.DataMessages.PublishMAVLink
import com.github.jodersky.flow.SerialSettings
import helios.core.clients.DataMessages.WriteMAVLink
import com.github.jodersky.flow.Serial.Received
import helios.messages.CoreMessages.SetSubscribers
import helios.types.ClientTypes.{FlightController, ClientType}

class MAVLinkUARTTest extends TestKit(ActorSystem("SerialPort"))
with FlatSpecLike
with BeforeAndAfterAll
with Matchers
with ImplicitSender {

  override def afterAll() = {
    system.shutdown()
  }

  lazy val settings = {
    val port = "/dev/ttyUSB0"
    val baud = 115200
    val cs = 8
    val tsb = false
    val parity = Parity(0)

    SerialSettings(port, baud, cs, tsb, parity)
  }

  lazy val heartbeat: MAVLinkMessage = {
    val hb = new msg_heartbeat(20, MAV_COMPONENT.MAV_COMP_ID_IMU)
    hb.sequence = 0
    hb.`type` = MAV_TYPE.MAV_TYPE_QUADROTOR
    hb.autopilot = MAV_AUTOPILOT.MAV_AUTOPILOT_GENERIC
    hb.base_mode = MAV_MODE.MAV_MODE_STABILIZE_ARMED
    hb.custom_mode = 0
    hb.system_status = MAV_STATE.MAV_STATE_STANDBY
    hb.mavlink_version = 3

    hb
  }

  lazy val operator = TestProbe()
  lazy val uartManager = TestProbe()
  lazy val recep = TestProbe()

  def ctf(a: ActorRef): ClientType = FlightController(a)

  def initUART: ActorRef = {
    val hu = system.actorOf(MAVLinkUART.props(ctf, uartManager.ref, settings))
    uartManager.expectMsg(Serial.Open(settings))
    uartManager.send(hu, Serial.Opened(settings, operator.ref))
    operator.expectMsg(Serial.Register(hu))

    hu
  }

  "MAVLinkUART" should "Open and register" in {
    //internal.InternalSerial.debug(true)
    initUART
  }

  it should "read data from the UART" in {
    val uartProxy = system.actorOf(Props(new Actor {
      val child = context.actorOf(MAVLinkUART.props(ctf, uartManager.ref, settings), "HeliosUARTwParent")
      def receive = {
        case x if sender == child =>
          println(s"Forwarding to self $x")
          testActor forward x
        case x =>
          println("Forwarding to child")
          child forward x
      }
    }))

    uartManager.expectMsg(Serial.Open(settings))
    uartManager.send(uartProxy, Serial.Opened(settings, operator.ref))
    operator.expectMsgClass(classOf[Serial.Register])

    expectMsgClass(classOf[RegisterClient])
    operator.send(uartProxy, SetSubscribers(Set(ctf(self))))

    val dataBs = ByteString(heartbeat.encode)

    operator.send(uartProxy, Received(dataBs))

    expectMsgClass(classOf[PublishMAVLink])
  }

  it should "not allow privileged messages to be sent from non-privileged senders" in {
    val probe = TestProbe()
    val sp = initUART

    val msg = new msg_set_roll_pitch_yaw_thrust(20, 0)

    probe.send(sp, WriteMAVLink(msg))
    operator.expectNoMsg(50 millis)
    probe.expectMsgClass(classOf[NotAllowed])
  }

  it should "not allow privileged commands to be sent from non-privileged senders" in {
    val probe = TestProbe()
    val sp = initUART

    val msg = new msg_command_long(20,0)

    PrivilegedLike.PrivilegedMAVLink.privilegedCommands.par.foreach { id =>
      msg.command = id
      probe.send(sp, WriteMAVLink(msg))
      operator.expectNoMsg(50 millis)
      probe.expectMsgClass(classOf[NotAllowed])
    }
  }

  it should "allow privileged messages to be sent from privileged senders" in {
    val probe = TestProbe()
    val sp = initUART
    probe.send(sp, SetPrimary(probe.ref))

    val msg = new msg_set_roll_pitch_yaw_thrust(20, 0)

    probe.send(sp, WriteMAVLink(msg))
    probe.expectNoMsg(50 millis)
    operator.expectMsgClass(classOf[Serial.Write])
  }

  it should "allow privileged commands to be sent from privileged senders" in {
    val probe = TestProbe()
    val sp = initUART
    probe.send(sp, SetPrimary(probe.ref))

    val msg = new msg_command_long(20,0)

    PrivilegedLike.PrivilegedMAVLink.privilegedCommands.par.foreach { id =>
      msg.command = id
      probe.send(sp, WriteMAVLink(msg))
      probe.expectNoMsg(50 millis)
      operator.expectMsgClass(classOf[Serial.Write])
    }
  }

  it should "terminate with Serial" in {
    val probe = TestProbe()

    val sp = initUART

    probe.watch(sp)
    probe.send(operator.ref, PoisonPill)
    probe.expectTerminated(sp)
  }
}
