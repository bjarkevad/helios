package helios.test

import org.scalatest._

import com.github.jodersky.flow._
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import akka.actor.{ActorRef, PoisonPill, ActorSystem}

import helios.core.actors.flightcontroller.FlightControllerMessages.WriteAck
import com.github.jodersky.flow.SerialSettings
import akka.io.IO
import scala.concurrent.duration._
import scala.language.postfixOps

import helios.core.actors.flightcontroller.HeliosUART
import akka.util.ByteString
import com.github.jodersky.flow.Serial.Received
import org.mavlink.messages._
import org.mavlink.messages.common.msg_heartbeat
import helios.core.actors.flightcontroller.FlightControllerMessages.WriteData
import com.github.jodersky.flow.Serial.Received
import helios.core.actors.flightcontroller.FlightControllerMessages.WriteAck
import com.github.jodersky.flow.SerialSettings
import helios.api.messages.MAVLinkMessages.PublishMAVLink

class HeliosUARTTest extends TestKit(ActorSystem("SerialPort"))
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
  lazy val uart = TestProbe()
  lazy val recep = TestProbe()

  def initUART: ActorRef = {
    val hu = system.actorOf(HeliosUART.props(recep.ref, uart.ref, settings))
    uart.expectMsg(Serial.Open(settings))
    uart.send(hu, Serial.Opened(settings, operator.ref))
    operator.expectMsg(Serial.Register(hu))

    hu
  }

  "HeliosUART" should "Open and register" in {
    //internal.InternalSerial.debug(true)
    val sp = initUART
  }

  it should "write data to the UART" in {
    val probe = TestProbe()

    val sp = initUART

    val data = "write this data"
    val dataBs = ByteString(data.getBytes)

    probe.send(sp, WriteData(data))
    operator.expectMsg(Serial.Write(dataBs, WriteAck(dataBs)))
  }

  it should "read data from the UART" in {
    val sp =initUART

    val dataBs = ByteString(heartbeat.encode)

    operator.send(sp, Received(dataBs))
    recep.expectMsgClass(classOf[PublishMAVLink])
  }

  it should "not allow any privileged messages to be sent from non-privileged senders" in {
    val sp = initUART
  }

  it should "terminate with Serial" in {
    val probe = TestProbe()

    val sp = initUART

    probe.watch(sp)
    probe.send(operator.ref, PoisonPill)
    probe.expectTerminated(sp)
  }
}
